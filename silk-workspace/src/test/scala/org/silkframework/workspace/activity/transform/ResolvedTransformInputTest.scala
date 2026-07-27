package org.silkframework.workspace.activity.transform

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.silkframework.entity.EntitySchema
import org.silkframework.entity.paths.{ForwardOperator, UntypedPath}
import org.silkframework.rule._
import org.silkframework.runtime.activity.TestUserContextTrait
import org.silkframework.util.Uri
import org.silkframework.workspace.TestWorkspaceProviderTestTrait
import org.silkframework.workspace.activity.transform.ResolvedTransformInput.StaticSchemaInput
import org.silkframework.workspace.activity.transform.TransformTaskUtils._

/**
  * Tests the resolution of transform inputs that are transform tasks themselves.
  */
class ResolvedTransformInputTest extends AnyFlatSpec with Matchers with TestWorkspaceProviderTestTrait with TestUserContextTrait {

  behavior of "ResolvedTransformInput"

  override def workspaceProviderId: String = "inMemoryWorkspaceProvider"

  private val sourceType = "urn:source:Person"
  private val rootType = "urn:target:Person"
  private val nestedType = "urn:target:Address"

  it should "resolve the schema of an upstream transform that reads a typed source" in {
    val project = retrieveOrCreateProject("typedUpstreamTest")
    project.addTask("upstream", upstreamTransform)
    project.addTask("downstream", TransformSpec(selection = DatasetSelection("upstream")))

    // The source type of the upstream transform must not be matched against its target types
    val input = project.task[TransformSpec]("downstream").resolveInput
    input mustBe a[StaticSchemaInput]
    val schema = input.asInstanceOf[StaticSchemaInput].schema
    schema.typeUri mustBe Uri(rootType)
    targetProperties(schema) must contain("urn:target:name")
  }

  it should "resolve the schema of the upstream rule that generates the selected type" in {
    val project = retrieveOrCreateProject("nestedUpstreamTest")
    project.addTask("upstream", upstreamTransform)
    project.addTask("downstream", TransformSpec(selection = DatasetSelection("upstream", Uri(nestedType))))

    val input = project.task[TransformSpec]("downstream").resolveInput
    val schema = input.asInstanceOf[StaticSchemaInput].schema
    schema.typeUri mustBe Uri(nestedType)
    targetProperties(schema) must contain("urn:target:city")
    targetProperties(schema) must not contain "urn:target:name"
  }

  /** An upstream transform that reads a typed source and maps it to a root and a nested object rule. */
  private def upstreamTransform: TransformSpec = {
    TransformSpec(
      selection = DatasetSelection("sourceDataset", Uri(sourceType)),
      mappingRule = RootMappingRule(MappingRules(
        typeRules = Seq(TypeMapping(id = "rootType", typeUri = rootType)),
        propertyRules = Seq(
          DirectMapping(id = "name", sourcePath = path("name"), mappingTarget = MappingTarget("urn:target:name")),
          ObjectMapping(
            id = "address",
            sourcePath = path("address"),
            target = Some(MappingTarget("urn:target:address")),
            rules = MappingRules(
              typeRules = Seq(TypeMapping(id = "addressType", typeUri = nestedType)),
              propertyRules = Seq(DirectMapping(id = "city", sourcePath = path("city"), mappingTarget = MappingTarget("urn:target:city")))
            )
          )
        )
      ))
    )
  }

  private def path(property: String): UntypedPath = UntypedPath(List(ForwardOperator(property)))

  private def targetProperties(schema: EntitySchema): Set[String] = {
    schema.typedPaths.flatMap(_.operators.collect { case ForwardOperator(property) => property.uri }).toSet
  }
}
