/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.is.data.sync.system.util;

import org.apache.axiom.om.util.Base64;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.core.util.CryptoException;
import org.wso2.carbon.core.util.CryptoUtil;
import org.wso2.carbon.crypto.api.CipherMetaDataHolder;
import org.wso2.is.data.sync.system.exception.SyncClientException;

import java.util.HashMap;
import java.util.Map;

/**
 * EncryptionUtil.
 */
public class EncryptionUtil {

    private static final Logger log = LoggerFactory.getLogger(EncryptionUtil.class);

    private static String oldEncryptionAlgorithmConfigured = null;
    private static Map<String, String> algorithmAndProviderMap = new HashMap<>();
    private static final String DEFAULT_OLD_ENCRYPTION_ALGORITHM = "RSA/ECB/OAEPwithSHA1andMGF1Padding";

    static {

        algorithmAndProviderMap.put("RSA", "org.wso2.carbon.crypto.provider.KeyStoreBasedInternalCryptoProvider");
        algorithmAndProviderMap.put("RSA/ECB/OAEPwithSHA1andMGF1Padding",
                "org.wso2.carbon.crypto.provider.KeyStoreBasedInternalCryptoProvider");
        algorithmAndProviderMap.put("AES", "org.wso2.carbon.crypto.provider.SymmetricKeyInternalCryptoProvider");
        algorithmAndProviderMap.put("AES/GCM/NoPadding", "org.wso2.carbon.crypto.provider" +
                ".SymmetricKeyInternalCryptoProvider");
    }

    public static boolean isNewlyEncrypted(String encryptedValue) throws CryptoException {

        return CryptoUtil.getDefaultCryptoUtil().base64DecodeAndIsSelfContainedCipherText(encryptedValue);
    }

    public static String transformToSymmetric(String currentEncryptedvalue) throws SyncClientException {

        try {
            String cryptoProvider = getInternalCryptoProviderFromAlgorithm(oldEncryptionAlgorithmConfigured);
            byte[] decryptedtext = CryptoUtil.getDefaultCryptoUtil()
                    .base64DecodeAndDecrypt(currentEncryptedvalue, oldEncryptionAlgorithmConfigured,
                            cryptoProvider);
            return CryptoUtil.getDefaultCryptoUtil().encryptAndBase64Encode(decryptedtext);
        } catch (CryptoException c) {
            log.warn(String.format("Error while decrypting using '%s'. The provided algorithm may be incorrect" +
                            ".Please check if your system have data encrypted with different algorithm.",
                    oldEncryptionAlgorithmConfigured));
            log.warn("Retrying decryption with self contained cipher.");
            retryDecryptionWithSuitableAlgorithm(currentEncryptedvalue);

        }

        return currentEncryptedvalue;
    }

    public static String retryDecryptionWithSuitableAlgorithm(String currentEncryptedvalue) throws SyncClientException {

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

            throw new SyncClientException(errorMsg, e);
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

    public static void setCurrentEncryptionAlgorithm(String algorithm) {

        oldEncryptionAlgorithmConfigured = algorithm;
        if (StringUtils.isBlank(oldEncryptionAlgorithmConfigured)) {
            oldEncryptionAlgorithmConfigured = DEFAULT_OLD_ENCRYPTION_ALGORITHM;
        }
    }

}
