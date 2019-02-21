package org.silkframework.runtime.users

/**
  * An authenticated user.
  */
trait AuthenticatedUser {
  /** Refreshes authentication tokens in token best authentication systems.
    * This can be called if a request with the current token returns a 401 (Not Authorized) response.
    * If refresh is not supported this method returns false.
    * @return true if the tokens has been refreshed, else false.
    */
  def refreshAuthentication(): Boolean

}
