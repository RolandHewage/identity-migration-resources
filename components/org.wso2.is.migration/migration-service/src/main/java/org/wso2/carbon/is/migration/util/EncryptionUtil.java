/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.is.migration.util;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.apache.axiom.om.util.Base64;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.core.util.CryptoException;
import org.wso2.carbon.core.util.CryptoUtil;
import org.wso2.carbon.crypto.api.CipherMetaDataHolder;
import org.wso2.carbon.identity.core.migrate.MigrationClientException;
import org.wso2.carbon.is.migration.config.Config;
import org.wso2.carbon.is.migration.service.Migrator;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/**
 * EncryptionUtil.
 */
public class EncryptionUtil {

    private static final Logger log = LoggerFactory.getLogger(EncryptionUtil.class);

    private static String oldEncryptionAlgorithmConfigured = null;
    private static String migratingEncryptionAlgorithmConfigured = null;
    static Map<String, String> algorithmAndProviderMap = new HashMap<>();

    static {

        algorithmAndProviderMap.put("RSA", "org.wso2.carbon.crypto.provider.KeyStoreBasedInternalCryptoProvider");
        algorithmAndProviderMap.put("RSA/ECB/OAEPwithSHA1andMGF1Padding",
                "org.wso2.carbon.crypto.provider.KeyStoreBasedInternalCryptoProvider");
        algorithmAndProviderMap.put("AES", "org.wso2.carbon.crypto.provider.SymmetricKeyInternalCryptoProvider");
        algorithmAndProviderMap.put("AES/GCM/NoPadding", "org.wso2.carbon.crypto.provider" +
                ".SymmetricKeyInternalCryptoProvider");
    }

    public static String getNewEncryptedValue(String encryptedValue) throws CryptoException {

        if (StringUtils.isNotEmpty(encryptedValue) && !isNewlyEncrypted(encryptedValue)) {
            byte[] decryptedPassword = CryptoUtil.getDefaultCryptoUtil().base64DecodeAndDecrypt(encryptedValue, "RSA");
            return CryptoUtil.getDefaultCryptoUtil().encryptAndBase64Encode(decryptedPassword);
        }
        return null;
    }

    public static boolean isNewlyEncrypted(String encryptedValue) throws CryptoException {

        return CryptoUtil.getDefaultCryptoUtil().base64DecodeAndIsSelfContainedCipherText(encryptedValue);
    }

    public static String getNewEncryptedUserstorePassword(String encryptedValue) throws CryptoException {

        if (StringUtils.isNotEmpty(encryptedValue) && !isNewlyEncryptedUserstorePassword(encryptedValue)) {
            byte[] decryptedPassword = SecondaryUserstoreCryptoUtil.getInstance()
                    .base64DecodeAndDecrypt(encryptedValue, "RSA");
            return SecondaryUserstoreCryptoUtil.getInstance().encryptAndBase64Encode(decryptedPassword);
        }
        return null;
    }

    public static boolean isNewlyEncryptedUserstorePassword(String encryptedValue) throws CryptoException {

        return SecondaryUserstoreCryptoUtil.getInstance().base64DecodeAndIsSelfContainedCipherText(encryptedValue);
    }

    public static String transformToSymmetric(String currentEncryptedvalue) throws MigrationClientException {

        try {
            if (StringUtils.isNotEmpty(currentEncryptedvalue) && isMigrationNeeded(Base64.decode
                    (currentEncryptedvalue))) {
                String cryptoProvider = getInternalCryptoProviderFromAlgorithm(oldEncryptionAlgorithmConfigured);
                byte[] decryptedtext = CryptoUtil.getDefaultCryptoUtil()
                        .base64DecodeAndDecrypt(currentEncryptedvalue, oldEncryptionAlgorithmConfigured,
                                cryptoProvider);
                return CryptoUtil.getDefaultCryptoUtil().encryptAndBase64Encode(decryptedtext);
            }
        } catch (CryptoException c) {
            log.warn(String.format("Error while decrypting using '%s'. The provided algorithm may be incorrect" +
                            ".Please check if your system have data encrypted with different algorithm.",
                    oldEncryptionAlgorithmConfigured));
            log.warn("Retrying decryption with self contained ciphe");
            retryDecryptionWithSuitableAlgorithm(currentEncryptedvalue);
        }
        return currentEncryptedvalue;
    }

    public static String retryDecryptionWithSuitableAlgorithm(String currentEncryptedvalue)
            throws MigrationClientException {

        CipherMetaDataHolder cipherMetaDataHolder =
                CryptoUtil.getDefaultCryptoUtil()
                        .cipherTextToCipherMetaDataHolder(Base64.decode(currentEncryptedvalue));
        String oldAlgorithm;

        if (cipherMetaDataHolder != null) {
            oldAlgorithm = cipherMetaDataHolder.getTransformation();
        } else {
            oldAlgorithm = "RSA";
        }
        log.info(String.format("Retrying decryption with '%s'. ", oldAlgorithm));
        try {
            byte[] decryptedtext = CryptoUtil.getDefaultCryptoUtil()
                    .base64DecodeAndDecrypt(currentEncryptedvalue, oldAlgorithm,
                            getInternalCryptoProviderFromAlgorithm(oldAlgorithm));
            return CryptoUtil.getDefaultCryptoUtil().encryptAndBase64Encode(decryptedtext);
        } catch (CryptoException e) {
            String errorMsg =
                    String.format("Error while decrypting using '%s'. The provided algorithm may be incorrect" +
                                    ".Please check if your system have data encrypted with different algorithm.",
                            oldAlgorithm);

            throw new MigrationClientException(errorMsg,e);
        }
    }

    private static String getInternalCryptoProviderFromAlgorithm(String algorithm) {

        for (Map.Entry<String, String> entry : algorithmAndProviderMap.entrySet()) {
            if (entry.getKey().equals(algorithm)) {
                return entry.getValue();
            }
        }
        return null;
    }

    public static void setCurrentEncryptionAlgorithm(Migrator migrator) {

        oldEncryptionAlgorithmConfigured = migrator.getMigratorConfig().getParameterValue(
                "currentEncryptionAlgorithm");
        if (StringUtils.isBlank(oldEncryptionAlgorithmConfigured)) {
            oldEncryptionAlgorithmConfigured = Config.getInstance().getCurrentEncryptionAlgorithm();
        }
    }

    public static String setMigratedEncryptionAlgorithm(Migrator migrator) {

        migratingEncryptionAlgorithmConfigured = migrator.getMigratorConfig().getParameterValue(
                "migratedEncryptionAlgorithm");
        if (StringUtils.isBlank(migratingEncryptionAlgorithmConfigured)) {
            return Config.getInstance().getMigratedEncryptionAlgorithm();
        }
        return migratingEncryptionAlgorithmConfigured;
    }


    public static boolean isMigrationNeeded(byte[] cipherText) {

        String cipherStr = new String(cipherText, Charset.defaultCharset());
        try {
            CipherMetaDataHolder
                    cipherMetaDataHolder = new Gson().fromJson(cipherStr, CipherMetaDataHolder.class);
            if (cipherMetaDataHolder != null) {
                if (log.isDebugEnabled()) {
                    log.debug(
                            String.format("Cipher text is in self contained format. Retrieving the actual cipher from" +
                                    " the self contained cipher text."));
                }
                String algorithm = cipherMetaDataHolder.getTransformation();
                if (migratingEncryptionAlgorithmConfigured.equals(algorithm)) {
                    return false;
                }
            }
        } catch (JsonSyntaxException e) {
            if (log.isDebugEnabled()) {
                log.debug("Deserialization failed since cipher string is not representing cipher with metadata");
            }
        }
        return true;
    }
}
