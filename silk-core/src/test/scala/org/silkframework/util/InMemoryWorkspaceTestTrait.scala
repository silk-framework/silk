package org.silkframework.util

import org.scalatest.Suite

/**
  * A trait that will configure the workspace to be in-memory.
  */
trait InMemoryWorkspaceTestTrait extends ConfigTestTrait {
  this: Suite =>
  /** The properties that should be changed.
    * If the value is [[None]] then the property value is removed,
    * else it is set to the new value.
    */
  override def propertyMap: Map[String, Option[String]] = Map(
    "workspace.provider.plugin" -> Some("inMemoryRdfWorkspace")
  )
}
