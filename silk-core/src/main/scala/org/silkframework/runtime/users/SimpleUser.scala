package org.silkframework.runtime.users

/**
  * A users that is only identified by its URI.
  */
case class SimpleUser(uri: String) extends User
