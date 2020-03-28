/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.is.data.sync.system.pipeline.transform;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The factory class for building {@link DataTransformer} instances associates for a sync pipeline based on the sync
 * table associated with the pipeline.
 */
public class DataTransformerFactory {

    private Log log = LogFactory.getLog(DataTransformerFactory.class);
    private List<DataTransformer> dataTransformers = new ArrayList<>();

    public DataTransformerFactory(List<DataTransformer> dataTransformers) {

        this.dataTransformers = dataTransformers;
    }

    /**
     * Builds and return an instance of {@link ChainDataTransformer} which contains an ordered list of data
     * transformers required for the data sync pipeline.
     *
     * @param tableName     Name of the data sync table.
     * @param sourceVersion Data sync source product version.
     * @param targetVersion Data sync target product version.
     * @return Instance of a ChainDataTransformer;
     */
    public DataTransformer buildTransformer(String tableName, String sourceVersion, String targetVersion) {

        ChainDataTransformer chainDataTransformer = new ChainDataTransformer();
        List<DataTransformer> tempDataTransformers = new ArrayList<>();
        boolean hasCustomTransformer = false;
        for (DataTransformer dataTransformer : dataTransformers) {

            VersionAdvice[] annotations = dataTransformer.getClass().getAnnotationsByType(VersionAdvice.class);
            for (VersionAdvice advice : annotations) {

                String version = advice.version();
                int tfVersion = resolveVersion(version);

                if (tfVersion < 0) {
                    log.warn("Custom transformer: " + dataTransformer.getClass().getName() + " supports an invalid " +
                            "product version: " + version);
                    continue;
                }
                boolean canTransform = canTransform(sourceVersion, targetVersion, tableName, advice, tfVersion);

                if (canTransform) {
                    tempDataTransformers.add(createNewInstance(dataTransformer));
                    log.info("Custom transformer: " + dataTransformer.getClass().getName() + " with version: " +
                            version + " available for table: " + tableName);
                    hasCustomTransformer = true;
                }
            }
        }
        if (!hasCustomTransformer) {
            log.info("Custom transformers not found for table: " + tableName + " using: PassThroughDataTransformer.");
            chainDataTransformer.add(new PassThroughDataTransformer());
        } else {
            tempDataTransformers.sort(Comparator.comparingInt(transformer -> getVersion(tableName, transformer)));
            chainDataTransformer.setDataTransformers(tempDataTransformers);
        }
        return chainDataTransformer;
    }

    private boolean canTransform(String sourceVersion, String targetVersion, String table, VersionAdvice advice,
                                 int tfVersion) {

        return advice.tableName().equals(table) &&
                (resolveVersion(sourceVersion) <= tfVersion) &&
                (resolveVersion(targetVersion) >= tfVersion);
    }

    private int resolveVersion(String version) {

        if ("5.0.0".equals(version)) {
            return 0;
        } else if ("5.1.0".equals(version)) {
            return 1;
        } else if ("5.2.0".equals(version)) {
            return 2;
        } else if ("5.3.0".equals(version)) {
            return 3;
        } else if ("5.4.0".equals(version)) {
            return 4;
        } else if ("5.5.0".equals(version)) {
            return 5;
        } else if ("5.6.0".equals(version)) {
            return 6;
        } else if ("5.7.0".equals(version)) {
            return 7;
        } else if ("5.8.0".equals(version)) {
            return 8;
        } else if ("5.9.0".equals(version)) {
            return 9;
        }
        return -1;
    }

    private DataTransformer createNewInstance(DataTransformer dataTransformer) {

        try {
            return dataTransformer.getClass().newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            log.error("Error while creating an instance of: " + dataTransformer.getClass().getName(), e);
        }
        return dataTransformer;
    }

    private int getVersion(String tableName, DataTransformer dataTransformer) {

        int version = 0;
        VersionAdvice[] versionAdvices = dataTransformer.getClass().getAnnotationsByType(VersionAdvice.class);
        for (VersionAdvice versionAdvice : versionAdvices) {
            if (versionAdvice.tableName().equals(tableName)) {
                version = resolveVersion(versionAdvice.version());
                break;
            }
        }
        return version;
    }
}
