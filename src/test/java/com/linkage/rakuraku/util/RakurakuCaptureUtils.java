package com.linkage.rakuraku.util;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import org.apache.commons.lang.StringUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import com.linkage.rakuraku.core.RakurakuCore;
import com.linkage.rakuraku.exp.RakurakuException;

public class RakurakuCaptureUtils {

    private String fileName = "/testresult/";

    private String defaultName = "GuiCamera";

    private int serialNum = 1;

    private String imageFormat = "jpg";

    public static String lastPicUrl = "";

    public static String nowPicName = "";

    public RakurakuCaptureUtils() {
        fileName = RakurakuFileUtils.getToolPath() + fileName + defaultName;
    }

    public RakurakuCaptureUtils(String fileName) {
        this.fileName = RakurakuCore.eachEviPath + fileName;
        new File(this.fileName.substring(0, this.fileName.lastIndexOf("/"))).mkdirs();
        lastPicUrl = "";
        nowPicName = "";
        RakurakuWebUtils.guiCamera = this;
    }

    /**
     * スクリームキャプチャ
     *
     * @param driver
     */
    public void snapshotFullScreen(WebDriver driver) throws Exception {
        driver = RakurakuWebUtils.webBeforeOperate(driver);
        String pic = fileName + StringUtils.leftPad(String.valueOf(serialNum), 3, "0") + "." + imageFormat;
        try {
            Thread.sleep(500);
            BufferedImage screen = null;
            String windowSize = RakurakuDBUtils.getProps().getProperty("DRIVER_SIZE");
            Rectangle rectangle = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
            if (StringUtils.isNotBlank(windowSize)) {
                int xPosition = driver.manage().window().getPosition().x;
                int yPosition = driver.manage().window().getPosition().y;
                String[] windows = windowSize.split(",");
                if (windows.length == 1) {
                    rectangle = new Rectangle(xPosition, yPosition, driver.manage().window().getSize().width,
                            driver.manage().window().getSize().height);
                } else if (windows.length == 2) {
                    if (StringUtils.isNotBlank(windows[0])) {
                        rectangle = new Rectangle(xPosition, yPosition, Integer.parseInt(windows[0]) + xPosition + 52,
                                Integer.parseInt(windows[1]) - yPosition);
                    } else {
                        rectangle = new Rectangle(xPosition, yPosition,
                                GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().width
                                        - xPosition,
                                Integer.parseInt(windows[1]) - yPosition);
                    }
                }
            }

            screen = (new Robot()).createScreenCapture(rectangle);
            screen = addWaterIcon(screen);
            serialNum++;
            File file = new File(pic);
            if (!file.exists()) {
                file.mkdirs();
            } else {
                file.delete();
            }
            lastPicUrl = driver.getCurrentUrl();
            nowPicName = file.getName();
            ImageIO.write(screen, imageFormat, file);
        } catch (Exception e) {
            throw new RakurakuException("キャプチャの保存に失敗しました。", e);
        }
    }

    /**
     * スクロールキャプチャ
     *
     * @param driver
     */
    public void snapshotScrollScreen(WebDriver driver) throws Exception {
        try {
            driver = RakurakuWebUtils.webBeforeOperate(driver);
            if (StringUtils.isNotBlank(RakurakuDBUtils.getProps().getProperty("IS_CHROME"))) {
                scrollShotForChrome(driver);
            } else {
                snapshotScrollScreenCommon(driver);
            }
        } catch (Exception e) {
            throw new RakurakuException("キャプチャの保存に失敗しました。", e);
        }
    }

    /**
     * WebDriverキャプチャ
     *
     * @param driver
     */
    public void snapshotScrollScreenCommon(WebDriver driver) throws Exception {
        String pic = fileName + StringUtils.leftPad(String.valueOf(serialNum), 3, "0") + "." + imageFormat;
        File srcFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        Image src = Toolkit.getDefaultToolkit().getImage(srcFile.getPath());
        BufferedImage screen = toBufferedImage(src);
        screen = addWaterIcon(screen);
        serialNum++;
        File file = new File(pic);
        if (file.exists()) {
            file.delete();
        }
        lastPicUrl = driver.getCurrentUrl();
        nowPicName = file.getName();
        ImageIO.write(screen, imageFormat, file);
    }

    public void scrollShotForChrome(WebDriver driver) throws Exception {
        String pic = fileName + StringUtils.leftPad(String.valueOf(serialNum), 3, "0") + "." + imageFormat;
        BufferedImage screen = RakurakuCaptureChromeUtils.captureFullSizePageForPC(imageFormat);
        screen = addWaterIcon(screen);
        serialNum++;
        File file = new File(pic);
        if (!file.exists()) {
            file.mkdirs();
        } else {
            file.delete();
        }
        lastPicUrl = driver.getCurrentUrl();
        nowPicName = file.getName();
        ImageIO.write(screen, imageFormat, file);
    }

    /**
     * キャプチャ番号増幅
     *
     * @return
     */
    public String getSeqNoPath() {
        String ret = fileName + StringUtils.leftPad(String.valueOf(serialNum), 3, "0");
        serialNum++;
        return ret;
    }

    /**
     * 水印追加
     *
     * @param screen
     * @return
     * @throws Exception
     */
    private BufferedImage addWaterIcon(BufferedImage screen) throws Exception {
        if (!"Y".equals(RakurakuDBUtils.getProps().getProperty("CAPTURE_WATER_ICON"))) {
            return screen;
        }
        Graphics2D g = screen.createGraphics();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, 0.5f));
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(screen, 0, 0, screen.getWidth(null), screen.getHeight(null), null);
        g.setColor(new Color(255, 165, 0)); // バックグラウンドカーラー設定
        g.fillRoundRect(screen.getWidth(null) - 430, 20, 420, 50, 15, 15);//
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, 1.0f));
        g.setColor(new Color(139, 0, 0)); // 水印カーラー設定
        g.setFont(new Font("Times New Roman", Font.BOLD, 25));// フォント設定
        g.drawString("Captured by Rakuraku 4.0.", screen.getWidth(null) - 410, 40);
        g.drawString(RakurakuDBUtils.getProps().getProperty("DEVICE_TYPE"), screen.getWidth(null) - 410, 65);
        g.dispose();
        return screen;
    }

    /**
     * BufferedImage取得
     *
     * @param image
     * @return
     */
    private BufferedImage toBufferedImage(Image image) {
        if (image instanceof BufferedImage) {
            return (BufferedImage) image;
        }
        // This code ensures that all the pixels in the image are loaded
        image = new ImageIcon(image).getImage();
        BufferedImage bimage = null;
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        try {
            int transparency = Transparency.OPAQUE;
            GraphicsDevice gs = ge.getDefaultScreenDevice();
            GraphicsConfiguration gc = gs.getDefaultConfiguration();
            bimage = gc.createCompatibleImage(image.getWidth(null), image.getHeight(null), transparency);
        } catch (HeadlessException e) {
            // The system does not have a screen
        }
        if (bimage == null) {
            // Create a buffered image using the default color model
            int type = BufferedImage.TYPE_INT_RGB;
            bimage = new BufferedImage(image.getWidth(null), image.getHeight(null), type);
        }
        // Copy image to buffered image
        Graphics g = bimage.createGraphics();
        // Paint the image onto the buffered image
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return bimage;
    }

}
