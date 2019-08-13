package org.silkframework.workspace.activity.transform

import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.entity.{StringValueType, UriValueType}
import org.silkframework.rule.TransformSpec
import org.silkframework.workspace.SingleProjectWorkspaceProviderTestTrait

class TransformPathsCacheTest extends FlatSpec with SingleProjectWorkspaceProviderTestTrait with MustMatchers {
  behavior of "Transform Path Cache"

  override def projectPathInClasspath: String = "diProjects/hierarchicalPersonJson.zip"

  override def workspaceProviderName: String = "inMemory"

  it should "cache typed paths for JSON data source" in {
    val task = project.task[TransformSpec]("personJsonTransform")
    val cache = task.activity[TransformPathsCache]
    cache.control.waitUntilFinished()
    val entitySchema = cache.value().configuredSchema
    for((path, expectedValueType) <- Seq(
      "id" -> StringValueType,
      "name" -> StringValueType,
      "phoneNumbers" -> UriValueType,
      "phoneNumbers/type" -> StringValueType,
      "phoneNumbers/number" -> StringValueType
    )) {
      val valueType = entitySchema.typedPaths.find(_.toUntypedPath.normalizedSerialization == path).map(_.valueType).getOrElse(
        throw new RuntimeException(s"Path $path was not found in cached entity schema!")
      )
      valueType mustBe expectedValueType
    }
  }
}
