package org.silkframework.runtime.plugin.types

import org.silkframework.runtime.resource.Resource

case class ResourceOption(resource: Option[Resource])

object ResourceOption {
  implicit def toResourceOption(v: Option[Resource]): ResourceOption = ResourceOption(v)
  implicit def fromResourceOption(v: ResourceOption): Option[Resource] = v.resource
}
