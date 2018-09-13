package org.silkframework.runtime.plugin

/**
  * A parameter that contains passwords and should be handled differently than plain text in various places, e.g. UI.
  */
case class PasswordParameter(str: String) {

  override def toString: String = str

  def decryptedString: String = str

}