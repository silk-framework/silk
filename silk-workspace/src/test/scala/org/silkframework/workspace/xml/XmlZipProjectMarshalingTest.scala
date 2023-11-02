package org.silkframework.workspace.xml


import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.config.{Prefixes, Tag}
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.rule.LinkSpec
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.annotations.Plugin
import org.silkframework.runtime.plugin.{PluginContext, PluginRegistry, TestPluginContext}
import org.silkframework.runtime.resource._
import org.silkframework.runtime.templating.{CompiledTemplate, TemplateEngine, TemplateVariableValue}
import org.silkframework.util.{ConfigTestTrait, Uri}
import org.silkframework.workspace.resources.InMemoryResourceRepository
import org.silkframework.workspace.{InMemoryWorkspaceProvider, Workspace}

import java.io.{File, FileOutputStream, Writer}
import java.nio.file.Files

/**
  * Tests for the XML zip based project marshalling
  */
class XmlZipProjectMarshalingTest extends AnyFlatSpec with Matchers with ConfigTestTrait {

  behavior of "Xml Zip project marshaling"

  implicit val userContext: UserContext = UserContext.Empty

  val projectName = "proj1"

  PluginRegistry.registerPlugin(classOf[MockTemplateEngine])

  it should "marshal and unmarshal correctly" in {
    val file = new File(getClass.getResource("exampleProject.zip").getFile)
    val marshaller = XmlZipWithResourcesProjectMarshaling()
    val resources = InMemoryResourceManager()
    val resourceRepository = InMemoryResourceRepository()
    val workspaceProvider = new InMemoryWorkspaceProvider()
    val workspace = new Workspace(workspaceProvider, resourceRepository)
    marshaller.unmarshalProject(projectName, workspaceProvider, resources, file)
    validateWorkspace(workspaceProvider, resources, requireResources = true)

    testRoundtrip(workspace, resources, exportResources = true, importResources = true)
    testRoundtrip(workspace, resources, exportResources = false, importResources = true)
    testRoundtrip(workspace, resources, exportResources = true, importResources = false)
  }

  private def testRoundtrip(workspace: Workspace, resources: ResourceManager,
                            exportResources: Boolean, importResources: Boolean) = {

    val exportMarshaller = XmlZipProjectMarshaling(exportResources)
    val project = workspace.projects.head
    val marshalledFile = Files.createTempFile("project", ".zip")
    val outputStream = new FileOutputStream(marshalledFile.toFile)
    try {
      exportMarshaller.marshalProject(project, outputStream, resources)
    } finally {
      outputStream.close()
    }

    val importMarshaller = XmlZipProjectMarshaling(importResources)
    val workspace2 = new InMemoryWorkspaceProvider()
    val resources2 = InMemoryResourceManager()
    importMarshaller.unmarshalProject(projectName, workspace2, resources2, marshalledFile.toFile)
    // Validate after complete round-trip
    validateWorkspace(workspace2, resources2, requireResources = exportResources && importResources)

    Files.delete(marshalledFile)
  }

  private def validateWorkspace(workspace: InMemoryWorkspaceProvider, resources: ResourceManager, requireResources: Boolean): Unit = {
    if(requireResources) {
      resources.list should contain allOf("source.csv", "target.csv")
    } else {
      resources.list shouldBe empty
    }

    implicit val pluginContext: PluginContext = TestPluginContext(prefixes = Prefixes.default, resources = resources)

    // Project
    val project = workspace.readProject(projectName).get

    // Variables
    val variables = workspace.projectVariables(projectName).readVariables()
    variables.map should contain key "linkLimit"
    variables.map should contain key "linkLimitTimesTen"
    variables.map("linkLimit").value shouldBe "1000"
    variables.map("linkLimitTimesTen").value shouldBe "10000"

    // Datasets
    val datasets = workspace.readTasks[GenericDatasetSpec](projectName)
    datasets.map(_.task.id.toString) should contain allOf("DBpedia", "linkedmdb")
    val dbpediaDataset = datasets.find(_.task.id.toString == "DBpedia").get.task
    val linkedmdbDataset = datasets.find(_.task.id.toString == "linkedmdb").get.task

    // Linking task
    val linkingTask = workspace.readTasks[LinkSpec](projectName)
    linkingTask.map(_.task.id.toString) should contain("movies")
    // Link limit is based on the linkLimitTimesTen template variable
    linkingTask.find(_.task.id.toString == "movies").get.task.data.linkLimit shouldBe 10000

    // Tags
    val tag1 = Tag(Uri("urn:silkframework:tag:example+tag+1"), "example tag 1")
    val tag2 = Tag(Uri("urn:silkframework:tag:example+tag+2"), "example tag 2")
    workspace.readTags(projectName) should contain theSameElementsAs Iterable(tag1, tag2)
    project.metaData.tags shouldBe Set(tag1.uri, tag2.uri)
    dbpediaDataset.metaData.tags shouldBe Set(tag1.uri, tag2.uri)
    linkedmdbDataset.metaData.tags shouldBe Set()
  }

  override def propertyMap: Map[String, Option[String]] = Map(
    "config.variables.engine" -> Some("test_template_engine")
  )

}

@Plugin(
  id = "test_template_engine",
  label = "Test Engine",
)
case class MockTemplateEngine() extends TemplateEngine {

  override def compile(templateString: String): CompiledTemplate = {
    templateString match {
      case "{{project.linkLimitTimesTen}}" =>
        new CompiledTemplate {
          override def evaluate(values: Seq[TemplateVariableValue], writer: Writer): Unit = {
            writer.write(values.find(_.name == "linkLimitTimesTen").get.values.mkString)
          }
        }
      case _ =>
        throw new IllegalArgumentException("Unexpected template: " + templateString)
    }
  }
}