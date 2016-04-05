package org.silkframework.workspace

case class ProjectNotFoundException(projectName: String) extends NoSuchElementException(s"Project '$projectName' not found")
