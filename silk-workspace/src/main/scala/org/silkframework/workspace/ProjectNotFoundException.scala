package org.silkframework.workspace

import org.silkframework.runtime.validation.NotFoundException

case class ProjectNotFoundException(projectName: String) extends NotFoundException(s"Project '$projectName' not found")
