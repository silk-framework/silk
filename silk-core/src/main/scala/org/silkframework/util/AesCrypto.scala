package org.silkframework.util

import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object AesCrypto {
  final val RANDOM_INIT_VECTOR = "FKJrJQWZ9DEW6KOv"

  def encrypt(key: String, value: String): String = {
    try {
      val iv = new IvParameterSpec(RANDOM_INIT_VECTOR.getBytes("UTF-8"))
      val skeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES")
      val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
      cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv)
      val encrypted = cipher.doFinal(value.getBytes)
      Base64.getEncoder.encodeToString(encrypted)
    }
  }

  def decrypt(key: String, encrypted: String): String = {
    try {
      val iv = new IvParameterSpec(RANDOM_INIT_VECTOR.getBytes("UTF-8"))
      val skeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES")
      val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
      cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv)
      val original = cipher.doFinal(Base64.getDecoder.decode(encrypted))
      new String(original)
    }
  }
}