package org.ergoplatform.api;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import org.jetbrains.annotations.NotNull;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

/**
 * Encryption / Decryption service using the AES algorithm
 * example for nullbeans.com
 */
public class AesEncryptionManager {

    public static final String MY_KEY_ALIAS = "ergowalletkey";
    public static final String ANDROID_KEY_STORE = "AndroidKeyStore";

    /**
     * This method will encrypt the given data using the given password.
     *
     * @param password the password that will be used to encrypt the data
     * @param data     the data that will be encrypted
     * @return Encrypted data in a byte array
     */
    public static byte[] encryptData(String password, byte[] data) throws NoSuchPaddingException,
            NoSuchAlgorithmException,
            InvalidAlgorithmParameterException,
            InvalidKeyException,
            BadPaddingException,
            IllegalBlockSizeException, InvalidKeySpecException {

        // Prepare the initialization vector (IV, aka `salt`)
        // IV should be 12 bytes
        SecureRandom secureRandom = new SecureRandom();
        byte[] iv = new byte[12];
        secureRandom.nextBytes(iv);

        //Prepare your key based on the password and the salt
        SecretKey secretKey = generateSecretKey(password, iv);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec parameterSpec = new GCMParameterSpec(128, iv);

        //Initialize the cipher for encryption mode!
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

        return encryptWithCipher(data, iv, cipher);
    }

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

        return encryptWithCipher(data, iv, cipher);
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

    /**
     * Encrypts the given data using the given {@link Cipher} and the salt.
     *
     * @param data   data to be encrypted
     * @param iv     initialization vector (aka salt)
     * @param cipher which is initialized for encryption
     * @return encrypted data packed with the salt.
     */
    @NotNull
    private static byte[] encryptWithCipher(byte[] data, byte[] iv, Cipher cipher) throws BadPaddingException, IllegalBlockSizeException {
        //Encrypt the data
        byte[] encryptedData = cipher.doFinal(data);

        //Concatenate everything and return the final data
        ByteBuffer byteBuffer = ByteBuffer.allocate(4 + iv.length + encryptedData.length);
        byteBuffer.putInt(iv.length);
        byteBuffer.put(iv);
        byteBuffer.put(encryptedData);
        return byteBuffer.array();
    }


    public static byte[] decryptData(String password, byte[] encryptedData)
            throws NoSuchPaddingException,
            NoSuchAlgorithmException,
            InvalidAlgorithmParameterException,
            InvalidKeyException,
            BadPaddingException,
            IllegalBlockSizeException,
            InvalidKeySpecException {

        DecryptionData data = new DecryptionData(encryptedData);

        //Prepare your key based on the password and the salt
        SecretKey secretKey = generateSecretKey(password, data.iv);

        return decryptWithSecretKey(data, secretKey);
    }

    private static byte[] decryptWithSecretKey(DecryptionData data, SecretKey secretKey) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec parameterSpec = new GCMParameterSpec(128, data.iv);

        //Encryption mode on!
        cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

        //decrypt the data
        return cipher.doFinal(data.cipherBytes);
    }

    public static byte[] decryptDataWithDeviceKey(byte[] encryptedData)
            throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException, UnrecoverableEntryException, IllegalBlockSizeException, InvalidKeyException, BadPaddingException, InvalidAlgorithmParameterException, NoSuchPaddingException {
        DecryptionData data = new DecryptionData(encryptedData);

        final SecretKey secretKey = loadSecretDeviceKey();
        return decryptWithSecretKey(data, secretKey);
    }

    private static SecretKey loadSecretDeviceKey() throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException, UnrecoverableEntryException {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
        keyStore.load(null);
        final KeyStore.SecretKeyEntry secretKeyEntry = (KeyStore.SecretKeyEntry) keyStore
                .getEntry(MY_KEY_ALIAS, null);

        return secretKeyEntry != null ? secretKeyEntry.getSecretKey() : null;
    }

    /**
     * Function to generate a 128 bit key from the given password and iv
     *
     * @param password used to derive the secret key
     * @param iv initialization vector (aka salt)
     * @return Secret key
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     */
    public static SecretKey generateSecretKey(String password, byte[] iv) throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeySpec spec = new PBEKeySpec(password.toCharArray(), iv, 65536, 128); // AES-128
        SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        byte[] key = secretKeyFactory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(key, "AES");
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

    private static class DecryptionData {
        final byte[] iv;
        final byte[] cipherBytes;

        DecryptionData(byte[] encryptedData) {
            //Wrap the data into a byte buffer to ease the reading process
            ByteBuffer byteBuffer = ByteBuffer.wrap(encryptedData);

            int nonceSize = byteBuffer.getInt();

            //Make sure that the file was encrypted properly
            if (nonceSize < 12 || nonceSize >= 16) {
                throw new IllegalArgumentException("Nonce size is incorrect. Make sure that the incoming data is an AES encrypted file.");
            }
            iv = new byte[nonceSize];
            byteBuffer.get(iv);

            //get the rest of encrypted data
            cipherBytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherBytes);
        }
    }
}