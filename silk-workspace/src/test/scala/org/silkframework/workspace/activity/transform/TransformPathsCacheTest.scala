package org.silkframework.workspace.activity.transform


import org.silkframework.entity.{StringValueType, UriValueType, ValueType}
import org.silkframework.entity.paths.{ForwardOperator, UntypedPath}
import org.silkframework.rule.{DatasetSelection, DirectMapping, MappingRules, MappingTarget, ObjectMapping, RootMappingRule, TransformSpec, TypeMapping}
import org.silkframework.util.Uri
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

  it should "cache the paths of the upstream rule that generates the selected type" in {
    val nestedType = "urn:target:Address"
    project.addTask[TransformSpec]("upstreamTransform", TransformSpec(
      selection = project.task[TransformSpec]("personJsonTransform").data.selection,
      mappingRule = RootMappingRule(MappingRules(
        typeRules = Seq(TypeMapping(id = "rootType", typeUri = "urn:target:Person")),
        propertyRules = Seq(
          DirectMapping("name", UntypedPath("name"), MappingTarget("urn:target:name")),
          ObjectMapping(
            id = "address",
            sourcePath = UntypedPath.empty,
            target = Some(MappingTarget("urn:target:address")),
            rules = MappingRules(
              typeRules = Seq(TypeMapping(id = "addressType", typeUri = nestedType)),
              propertyRules = Seq(DirectMapping("city", UntypedPath("id"), MappingTarget("urn:target:city")))
            )
          )
        )
      ))
    ))
    project.addTask[TransformSpec]("downstreamTransform", TransformSpec(
      selection = DatasetSelection("upstreamTransform", Uri(nestedType))
    ))
    val cache = project.task[TransformSpec]("downstreamTransform").activity[TransformPathsCache]
    cache.control.waitUntilFinished()

    // The editor must offer the paths of the rule that generates the selected type, not those of the root rule
    val properties = cache.value().configuredSchema.typedPaths.flatMap(_.operators.collect { case ForwardOperator(p) => p.uri })
    properties must contain("urn:target:city")
    properties must not contain "urn:target:name"
  }
}
