// Licensed to the Software Freedom Conservancy (SFC) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The SFC licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.openqa.selenium.firefox;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import com.google.common.io.CharStreams;
import com.google.common.io.LineReader;

import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.io.IOUtils;
import org.openqa.selenium.remote.JsonToBeanConverter;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


class Preferences {

  /**
   * The maximum amount of time scripts should be permitted to run. The user may increase this
   * timeout, but may not set it below the default value.
   */
  private static final String MAX_SCRIPT_RUN_TIME_KEY = "dom.max_script_run_time";
  private static final int DEFAULT_MAX_SCRIPT_RUN_TIME = 30;

  /**
   * This pattern is used to parse preferences in user.js. It is intended to match all preference
   * lines in the format generated by Firefox; it won't necessarily match all possible lines that
   * Firefox will parse.
   *
   * e.g. if you have a line with extra spaces after the end-of-line semicolon, this pattern will
   * not match that line because Firefox never generates lines like that.
   */
  private static final Pattern PREFERENCE_PATTERN =
      Pattern.compile("user_pref\\(\"([^\"]+)\", (\"?.+?\"?)\\);");

  private Map<String, Object> immutablePrefs = Maps.newHashMap();
  private Map<String, Object> allPrefs = Maps.newHashMap();

  public Preferences(Reader defaults) {
    readDefaultPreferences(defaults);
  }

  public Preferences(Reader defaults, File userPrefs) {
    readDefaultPreferences(defaults);
    FileReader reader = null;
    try {
      reader = new FileReader(userPrefs);
      readPreferences(reader);
    } catch (IOException e) {
      throw new WebDriverException(e);
    } finally {
      IOUtils.closeQuietly(reader);
    }
  }

  public Preferences(Reader defaults, Reader reader) {
    readDefaultPreferences(defaults);
    try {
      readPreferences(reader);
    } catch (IOException e) {
      throw new WebDriverException(e);
    } finally {
      IOUtils.closeQuietly(reader);
    }
  }

  private void readDefaultPreferences(Reader defaultsReader) {
    try {
      String rawJson = CharStreams.toString(defaultsReader);
      Map<String, Object> map = new JsonToBeanConverter().convert(Map.class, rawJson);

      Map<String, Object> frozen = (Map<String, Object>) map.get("frozen");
      for (Map.Entry<String, Object> entry : frozen.entrySet()) {
        String key = entry.getKey();
        Object value = entry.getValue();
        if (value instanceof Long) {
          value = new Integer(((Long)value).intValue());
        }
        setPreference(key, value);
        immutablePrefs.put(key, value);
      }

      Map<String, Object> mutable = (Map<String, Object>) map.get("mutable");
      for (Map.Entry<String, Object> entry : mutable.entrySet()) {
        Object value = entry.getValue();
        if (value instanceof Long) {
          value = new Integer(((Long)value).intValue());
        }
        setPreference(entry.getKey(), value);
      }
    } catch (IOException e) {
      throw new WebDriverException(e);
    }
  }

  private void setPreference(String key, Object value) {
    if (value instanceof String) {
      setPreference(key, (String) value);
    } else if (value instanceof Boolean) {
      setPreference(key, ((Boolean) value).booleanValue());
    } else {
      setPreference(key, ((Number) value).intValue());
    }
  }

  private void readPreferences(Reader reader) throws IOException {
    LineReader allLines = new LineReader(reader);
    String line = allLines.readLine();
    while (line != null) {
      Matcher matcher = PREFERENCE_PATTERN.matcher(line);
      if (matcher.matches()) {
        allPrefs.put(matcher.group(1), preferenceAsValue(matcher.group(2)));
      }
      line = allLines.readLine();
    }
  }

  public void setPreference(String key, String value) {
    checkPreference(key, value);
    if (isStringified(value)) {
      throw new IllegalArgumentException(
          String.format("Preference values must be plain strings: %s: %s",
              key, value));
    }
    allPrefs.put(key, value);
  }

  public void setPreference(String key, boolean value) {
    checkPreference(key, value);
    allPrefs.put(key, value);
  }

  public void setPreference(String key, int value) {
    checkPreference(key, value);
    allPrefs.put(key, value);
  }

  public void addTo(Preferences prefs) {
    // TODO(simon): Stop being lazy
    prefs.allPrefs.putAll(allPrefs);
  }

  public void addTo(FirefoxProfile profile) {
    profile.getAdditionalPreferences().allPrefs.putAll(allPrefs);
  }

  public void writeTo(Writer writer) throws IOException {
    for (Map.Entry<String, Object> pref : allPrefs.entrySet()) {
      writer.append("user_pref(\"").append(pref.getKey()).append("\", ");
      writer.append(valueAsPreference(pref.getValue()));
      writer.append(");\n");
    }
  }

  private String valueAsPreference(Object value) {
    if (value instanceof String) {
      return "\"" + escapeValueAsPreference((String) value) + "\"";
    } else {
      return escapeValueAsPreference(String.valueOf(value));
    }

  }

  private String escapeValueAsPreference(String value) {
    return value.replaceAll("\\\\", "\\\\\\\\").replaceAll("\"", "\\\\\"");
  }

  private Object preferenceAsValue(String toConvert) {
    if (toConvert.startsWith("\"") && toConvert.endsWith("\"")) {
      return toConvert.substring(1, toConvert.length() - 1).replaceAll("\\\\\\\\", "\\\\");
    }

    if ("false".equals(toConvert) || "true".equals(toConvert)) {
      return Boolean.parseBoolean(toConvert);
    }

    try {
      return Integer.parseInt(toConvert);
    } catch (NumberFormatException e) {
      throw new WebDriverException(e);
    }
  }

  @VisibleForTesting
  protected Object getPreference(String key) {
    return allPrefs.get(key);
  }

  private boolean isStringified(String value) {
    // Assume we a string is stringified (i.e. wrapped in " ") when
    // the first character == " and the last character == "
    return value.startsWith("\"") && value.endsWith("\"");
  }

  public void putAll(Map<String, Object> frozenPreferences) {
    allPrefs.putAll(frozenPreferences);
  }

  private void checkPreference(String key, Object value) {
    checkNotNull(value);
    checkArgument(!immutablePrefs.containsKey(key) ||
                  (immutablePrefs.containsKey(key) && value.equals(immutablePrefs.get(key))),
                  "Preference %s may not be overridden: frozen value=%s, requested value=%s",
                  key, immutablePrefs.get(key), value);
    if (MAX_SCRIPT_RUN_TIME_KEY.equals(key)) {
      int n;
      if (value instanceof String) {
        n = Integer.parseInt((String) value);
      } else if (value instanceof Integer) {
        n = (Integer) value;
      } else {
        throw new IllegalArgumentException(String.format(
            "%s value must be a number: %s", MAX_SCRIPT_RUN_TIME_KEY, value.getClass().getName()));
      }
      checkArgument(n == 0 || n >= DEFAULT_MAX_SCRIPT_RUN_TIME,
                    "%s must be == 0 || >= %s",
                    MAX_SCRIPT_RUN_TIME_KEY,
                    DEFAULT_MAX_SCRIPT_RUN_TIME);
    }
  }

}
