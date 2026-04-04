package com.wallparisoft.authentication.util;

import java.nio.charset.StandardCharsets;
import java.security.spec.KeySpec;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AESEncryptionUtil {

  private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
  private static final String SECRET_KEY_ALGORITHM = "PBKDF2WithHmacSHA256";

  @Value("${security.password.encryption.secret:W@llp@r1S0ft2024Secret!}")
  private String secretKey;

  @Value("${security.password.encryption.salt:W@llp@r1S@lt2024}")
  private String salt;

  /**
   * Encripta una contraseña en texto plano usando AES-256
   *
   * @param plainPassword Contraseña en texto plano
   * @return Contraseña encriptada en Base64
   */
  public String encrypt(String plainPassword) {
    try {
      SecretKey key = generateKey();
      Cipher cipher = Cipher.getInstance(ALGORITHM);
      IvParameterSpec iv =
          new IvParameterSpec(salt.substring(0, 16).getBytes(StandardCharsets.UTF_8));
      cipher.init(Cipher.ENCRYPT_MODE, key, iv);
      byte[] encrypted = cipher.doFinal(plainPassword.getBytes(StandardCharsets.UTF_8));
      return Base64.getEncoder().encodeToString(encrypted);
    } catch (Exception e) {
      log.error("Error al encriptar contraseña", e);
      throw new RuntimeException("Error al encriptar contraseña", e);
    }
  }

  /**
   * Desencripta una contraseña encriptada con AES-256
   *
   * @param encryptedPassword Contraseña encriptada en Base64
   * @return Contraseña en texto plano
   */
  public String decrypt(String encryptedPassword) {
    try {
      SecretKey key = generateKey();
      Cipher cipher = Cipher.getInstance(ALGORITHM);
      IvParameterSpec iv =
          new IvParameterSpec(salt.substring(0, 16).getBytes(StandardCharsets.UTF_8));
      cipher.init(Cipher.DECRYPT_MODE, key, iv);
      byte[] decodedBytes = Base64.getDecoder().decode(encryptedPassword);
      byte[] decrypted = cipher.doFinal(decodedBytes);
      return new String(decrypted, StandardCharsets.UTF_8);
    } catch (Exception e) {
      log.error("Error al desencriptar contraseña", e);
      throw new RuntimeException("Error al desencriptar contraseña", e);
    }
  }

  /** Genera la clave secreta a partir del secretKey y salt */
  private SecretKey generateKey() throws Exception {
    SecretKeyFactory factory = SecretKeyFactory.getInstance(SECRET_KEY_ALGORITHM);
    KeySpec spec = new PBEKeySpec(secretKey.toCharArray(), salt.getBytes(), 65536, 256);
    SecretKey tmp = factory.generateSecret(spec);
    return new SecretKeySpec(tmp.getEncoded(), "AES");
  }
}
