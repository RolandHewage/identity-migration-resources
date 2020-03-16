package org.wso2.carbon.is.migration.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

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
            fileName = UUID.randomUUID().toString() + ".txt";
            path = Paths.get(filePath, fileName);
            Files.createFile(path);
        }

        Files.write(path, builder.toString().getBytes(), StandardOpenOption.APPEND);
        builder = new StringBuilder();
    }
}
