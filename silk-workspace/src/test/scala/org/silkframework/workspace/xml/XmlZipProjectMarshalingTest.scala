package org.silkframework.workspace.xml

import java.io.{File, FileOutputStream}
import java.nio.file.Files

import org.scalatest.{FlatSpec, Matchers}
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.rule.LinkSpec
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource._
import org.silkframework.workspace.resources.InMemoryResourceRepository
import org.silkframework.workspace.{InMemoryWorkspaceProvider, Workspace}

/**
  * Tests for the XML zip based project marshalling
  */
class XmlZipProjectMarshalingTest extends FlatSpec with Matchers {

  behavior of "Xml Zip project marshaling"

  implicit val userContext: UserContext = UserContext.Empty

  val projectName = "proj1"

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
    val datasets = workspace.readTasks[GenericDatasetSpec](projectName, resources)
    val linkingTask = workspace.readTasks[LinkSpec](projectName, resources)
    datasets.map(_.task.id.toString) should contain allOf("DBpedia", "linkedmdb")
    linkingTask.map(_.task.id.toString) should contain("movies")
  }
}
