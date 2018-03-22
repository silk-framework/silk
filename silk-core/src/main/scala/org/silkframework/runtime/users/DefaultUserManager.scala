package org.silkframework.runtime.users

/**
  * Created by robert on 6/28/2016.
  */
object DefaultUserManager {

  def get(uri: String): User = {
    new DefaultUser(uri)
  }

  private class DefaultUser(val uri: String) extends User

}
