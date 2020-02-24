package org.silkframework.runtime.plugin

import java.security.InvalidKeyException
import java.util.logging.Logger

import javax.crypto.BadPaddingException
import org.silkframework.execution.{AbortExecutionException, ExecutionException}
import org.silkframework.runtime.plugin.ParameterType.PasswordParameterType
import org.silkframework.util.AesCrypto

/**
  * A parameter that contains passwords and should be handled differently than plain text in various places, e.g. UI.
  *
  * @param str The AES encrypted Base64-encoded password
  */
case class PasswordParameter(str: String) {
  private val log: Logger = Logger.getLogger(getClass.getName)

  override def toString: String = if(str == null || str == "") {
    str // Handle empty string as empty password and vice versa
  } else {
    PasswordParameterType.PREAMBLE + str
  }

  def decryptedString: String = {
    if(str == null || str == "") {
      str // Handle empty string as empty password and vice versa
    } else {
      try {
        AesCrypto.decrypt(PasswordParameterType.key, str)
      } catch {
        case ex: InvalidKeyException =>
          throw AbortExecutionException(s"The password parameter encryption key is invalid. Value for " +
              s"${PasswordParameterType.CONFIG_KEY} needs to be a character string of length 16.", cause = Some(ex))
        case _: BadPaddingException =>
          throw AbortExecutionException(s"Password parameter value could not be decrypted. If the value for config key ${PasswordParameterType.CONFIG_KEY} has been changed, " +
              s"all passwords for the operator need to be re-entered.")
      }
    }
  }
}

object PasswordParameter {

  def empty: PasswordParameter = PasswordParameter("")

}
