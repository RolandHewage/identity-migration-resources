/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.is.migration.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Utility class to generate a file based report.
 */
public class ReportUtil {

    private static final Logger log = LoggerFactory.getLogger(ReportUtil.class);
    private static final int MAX_LENGTH = 10000000;

    private String fileName;
    private String filePath;
    private StringBuilder builder = new StringBuilder();

    public ReportUtil(String filePath) {

        this.filePath = filePath;
    }

    public void writeMessage(String message) {

        writeMessage(message, true);
    }

    public void writeMessage(String message, boolean addNewlineAtEnd) {

        builder.append(message);
        if (addNewlineAtEnd) {
            builder.append("\n");
        }

        if (builder.length() > MAX_LENGTH) {
            Thread thread = new Thread(() -> {
                try {
                    commit();
                    log.info("Report file with name {} created inside {}.", fileName, filePath);
                } catch (IOException e) {
                    log.error("Error while writing the report file.", e);
                }
            });
            thread.start();
        }
    }

    public synchronized void commit() throws IOException {

        commit(false);
    }

    public synchronized void commit(boolean newFile) throws IOException {

        Path path;
        if (fileName != null) {
            path = Paths.get(filePath, fileName);
        } else {
            path = Paths.get(filePath);
        }

        if (Files.isDirectory(path) || Files.notExists(path) || newFile) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss");
            fileName = sdf.format(new Date()) + ".txt";
            path = Paths.get(filePath, fileName);
            Files.createFile(path);
        }

        Files.write(path, builder.toString().getBytes(), StandardOpenOption.APPEND);
        builder = new StringBuilder();
    }
}
