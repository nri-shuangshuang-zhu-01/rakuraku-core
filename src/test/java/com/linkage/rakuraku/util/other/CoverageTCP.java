package com.linkage.rakuraku.util.other;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import org.jacoco.core.data.ExecutionDataWriter;
import org.jacoco.core.runtime.RemoteControlReader;
import org.jacoco.core.runtime.RemoteControlWriter;

public class CoverageTCP {

    public static void main(String[] args) throws IOException {

        final FileOutputStream localFile = new FileOutputStream("D:\\pleiades\\workspace\\rakuraku-auto-test\\testresult\\2021-07-13\\mockApiDemo\\mockApiDemo_2021-07-13.exec");
        final ExecutionDataWriter localWriter = new ExecutionDataWriter(localFile);

        final Socket socket = new Socket(InetAddress.getByName("localhost"), 8395);
        final RemoteControlWriter writer = new RemoteControlWriter(socket.getOutputStream());
        final RemoteControlReader reader = new RemoteControlReader(socket.getInputStream());

        reader.setSessionInfoVisitor(localWriter);
        reader.setExecutionDataVisitor(localWriter);

        writer.visitDumpCommand(true, false);
        reader.read();

        socket.close();
        localFile.close();
    }

}
