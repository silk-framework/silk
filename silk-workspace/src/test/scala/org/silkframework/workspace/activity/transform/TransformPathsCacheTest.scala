package org.silkframework.workspace.activity.transform


import org.silkframework.entity.{StringValueType, UriValueType, ValueType}
import org.silkframework.rule.TransformSpec
import org.silkframework.workspace.SingleProjectWorkspaceProviderTestTrait
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class TransformPathsCacheTest extends AnyFlatSpec with SingleProjectWorkspaceProviderTestTrait with Matchers {
  behavior of "Transform Path Cache"

  override def projectPathInClasspath: String = "diProjects/hierarchicalPersonJson.zip"

  override def workspaceProviderId: String = "inMemoryWorkspaceProvider"

  it should "cache typed paths for JSON data source" in {
    val task = project.task[TransformSpec]("personJsonTransform")
    val cache = task.activity[TransformPathsCache]
    cache.control.waitUntilFinished()
    val entitySchema = cache.value().configuredSchema
    for((path, expectedValueType) <- Seq(
      "id" -> ValueType.STRING,
      "name" -> ValueType.STRING,
      "phoneNumbers" -> ValueType.URI,
      "phoneNumbers/type" -> ValueType.STRING,
      "phoneNumbers/number" -> ValueType.STRING
    )) {
      val valueType = entitySchema.typedPaths.find(_.toUntypedPath.normalizedSerialization == path).map(_.valueType).getOrElse(
        throw new RuntimeException(s"Path $path was not found in cached entity schema!")
      )
      valueType mustBe expectedValueType
    }
  }
}
