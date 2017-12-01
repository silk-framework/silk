package org.silkframework.runtime.users

/**
  * A user of the Silk web interface.
  */
case class WebUser(uri: String, name: Option[String]) extends User