package org.silkframework.runtime.plugin

import org.silkframework.runtime.plugin.ParameterType.PasswordParameterType
import org.silkframework.util.AesCrypto

/**
  * A parameter that contains passwords and should be handled differently than plain text in various places, e.g. UI.
  *
  * @param str The AES encrypted Base64-encoded password
  */
case class PasswordParameter(str: String) {

  override def toString: String = if(str == null || str == "") {
    str // Handle empty string as empty password and vice versa
  } else {
    PasswordParameterType.PREAMBLE + str
  }

  def decryptedString: String = {
    if(str == null || str == "") {
      str // Handle empty string as empty password and vice versa
    } else {
      AesCrypto.decrypt(PasswordParameterType.key, str)
    }
  }
}

object PasswordParameter {

  def empty: PasswordParameter = PasswordParameter("")

}
