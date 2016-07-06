package org.silkframework.runtime.users

/**
  * Created by robert on 6/28/2016.
  */
object DefaultUserManager {

  def get(id: String): User = {
    new DefaultUser(id)
  }

  private class DefaultUser(val id: String) extends User

}
