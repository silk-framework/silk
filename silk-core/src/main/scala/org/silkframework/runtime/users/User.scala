package org.silkframework.runtime.users

trait User {

  /** The URI of the user */
  def uri: String

  /** user information that can appear in logs to debug issues  */
  def logInfo: String = s"User(URI = $uri)"

}
