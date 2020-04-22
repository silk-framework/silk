package org.silkframework.util

import java.util.Base64

import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import java.security.spec.InvalidKeySpecException
import javax.crypto.spec.SecretKeySpec

object AesCrypto {

  private final val INIT_VECTOR = new IvParameterSpec("FKJrJQWZ9DEW6KOv".getBytes("UTF-8"))

  private final val SALT = Array[Byte](9, -119, -42, 5, -63, 102, -11, -104, 66, -17, 112, 55, 44, 73, 32, -12, -103, 88, 14, -44, 18, -46, 30, -6, -55, 28, -54, 12, 39, 110, 63, 125)

  /**
    * Generates a secret key from a password.
    *
    * @throws InvalidKeySpecException If no key could be generated from the password.
    */
  def generateKey(password: String): SecretKey = {
    val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
    val spec = new PBEKeySpec(password.toCharArray, SALT, 65536, 256)
    val secret = factory.generateSecret(spec)
    new SecretKeySpec(secret.getEncoded, "AES")
  }

  def encrypt(key: SecretKey, value: String): String = {
    val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
    cipher.init(Cipher.ENCRYPT_MODE, key, INIT_VECTOR)
    val encrypted = cipher.doFinal(value.getBytes)
    Base64.getEncoder.encodeToString(encrypted)
  }

  def decrypt(key: SecretKey, encrypted: String): String = {
    val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
    cipher.init(Cipher.DECRYPT_MODE, key, INIT_VECTOR)
    val original = cipher.doFinal(Base64.getDecoder.decode(encrypted))
    new String(original)
  }
}