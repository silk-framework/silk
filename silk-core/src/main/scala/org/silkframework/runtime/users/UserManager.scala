package org.silkframework.runtime.users

trait UserManager {

  def get(id: String): User

}
