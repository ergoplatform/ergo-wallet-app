package org.ergoplatform.api;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

public class AndroidEncryptionManager {
    public static final String MY_KEY_ALIAS = "ergowalletkey";
    public static final String ANDROID_KEY_STORE = "AndroidKeyStore";

    public static byte[] encryptDataOnDevice(byte[] data) throws NoSuchPaddingException,
            NoSuchAlgorithmException,
            InvalidAlgorithmParameterException,
            InvalidKeyException,
            BadPaddingException,
            IllegalBlockSizeException, NoSuchProviderException, CertificateException, UnrecoverableEntryException, KeyStoreException, IOException {

        SecretKey secretKey = loadSecretDeviceKey();
        if (secretKey == null) {
            // first use, we need to generate the key
            secretKey = generateDeviceSecretKey();
        }

        final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] iv = cipher.getIV();

        return AesEncryptionManager.encryptWithCipher(data, iv, cipher);
    }

    private static SecretKey generateDeviceSecretKey() throws NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException {
        SecretKey secretKey;
        final KeyGenerator keyGenerator = KeyGenerator
                .getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE);

        final KeyGenParameterSpec keyGenParameterSpec = new KeyGenParameterSpec.Builder(MY_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setUserAuthenticationRequired(true)
                .setUserAuthenticationValidityDurationSeconds(10)
                .build();

        keyGenerator.init(keyGenParameterSpec);
        secretKey = keyGenerator.generateKey();
        return secretKey;
    }

    public static byte[] decryptDataWithDeviceKey(byte[] encryptedData)
            throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException, UnrecoverableEntryException, IllegalBlockSizeException, InvalidKeyException, BadPaddingException, InvalidAlgorithmParameterException, NoSuchPaddingException {
        AesEncryptionManager.DecryptionData data = new AesEncryptionManager.DecryptionData(encryptedData);

        final SecretKey secretKey = loadSecretDeviceKey();
        return AesEncryptionManager.decryptWithSecretKey(data, secretKey);
    }

    private static SecretKey loadSecretDeviceKey() throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException, UnrecoverableEntryException {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
        keyStore.load(null);
        final KeyStore.SecretKeyEntry secretKeyEntry = (KeyStore.SecretKeyEntry) keyStore
                .getEntry(MY_KEY_ALIAS, null);

        return secretKeyEntry != null ? secretKeyEntry.getSecretKey() : null;
    }

    public static void emptyKeystore() {
        try {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);
            keyStore.deleteEntry(MY_KEY_ALIAS);
        } catch (IOException | CertificateException | NoSuchAlgorithmException | KeyStoreException e) {
            e.printStackTrace();
        }
    }
}
