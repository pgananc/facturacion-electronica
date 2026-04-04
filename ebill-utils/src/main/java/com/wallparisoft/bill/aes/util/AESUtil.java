package com.wallparisoft.bill.aes.util;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec; // Para el modo GCM
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class AESUtil {

  private static final int SALT_LENGTH = 16; // bytes
  private static final int IV_LENGTH = 12; // GCM recommended IV length
  private static final int KEY_LENGTH = 256; // bits
  private static final int ITERATIONS = 65536;
  private static final String ENCRYPT_ALGO = "AES/GCM/NoPadding";

  public static String encrypt(String plainText, char[] password) throws Exception {
    byte[] salt = generateRandomBytes(SALT_LENGTH);
    byte[] iv = generateRandomBytes(IV_LENGTH);

    SecretKey key = deriveKey(password, salt);

    Cipher cipher = Cipher.getInstance(ENCRYPT_ALGO);
    GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);
    cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec);

    byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

    // Concatenate salt + iv + cipherText and encode in Base64
    byte[] encryptedData = new byte[salt.length + iv.length + cipherText.length];
    System.arraycopy(salt, 0, encryptedData, 0, salt.length);
    System.arraycopy(iv, 0, encryptedData, salt.length, iv.length);
    System.arraycopy(cipherText, 0, encryptedData, salt.length + iv.length, cipherText.length);

    return Base64.getEncoder().encodeToString(encryptedData);
  }

  public static String decrypt(String encryptedData, char[] password) throws Exception {
    byte[] decoded = Base64.getDecoder().decode(encryptedData);

    byte[] salt = new byte[SALT_LENGTH];
    byte[] iv = new byte[IV_LENGTH];
    byte[] cipherText = new byte[decoded.length - SALT_LENGTH - IV_LENGTH];

    System.arraycopy(decoded, 0, salt, 0, SALT_LENGTH);
    System.arraycopy(decoded, SALT_LENGTH, iv, 0, IV_LENGTH);
    System.arraycopy(decoded, SALT_LENGTH + IV_LENGTH, cipherText, 0, cipherText.length);

    SecretKey key = deriveKey(password, salt);

    Cipher cipher = Cipher.getInstance(ENCRYPT_ALGO);
    GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);
    cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec);

    byte[] plainTextBytes = cipher.doFinal(cipherText);
    return new String(plainTextBytes, StandardCharsets.UTF_8);
  }

  private static SecretKey deriveKey(char[] password, byte[] salt)
      throws NoSuchAlgorithmException, InvalidKeySpecException {
    SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
    PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH);
    SecretKey tmp = factory.generateSecret(spec);
    return new SecretKeySpec(tmp.getEncoded(), "AES");
  }

  private static byte[] generateRandomBytes(int length) throws NoSuchAlgorithmException {
    byte[] bytes = new byte[length];
    SecureRandom sr = SecureRandom.getInstanceStrong();
    sr.nextBytes(bytes);
    return bytes;
  }
}
