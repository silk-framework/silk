package org.silkframework.execution.typed

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.config.{PlainTask, Prefixes}
import org.silkframework.dataset.{DatasetSpec, EmptyDataset}
import org.silkframework.execution.local.GenericEntityTable
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.resource.{FileResourceManager, ResourceManager}
import org.silkframework.util.FileUtils.toFileUtils

import java.nio.file.Files

class FileEntitySchemaTest extends AnyFlatSpec with Matchers {

  behavior of "FileEntitySchema"

  it should "be converted to a generic entity schema and read back to file entities" in {
    // Create a project file
    val resources = createTempResourceManager()
    val projectFile = FileEntity(resources.get("testProjectFile"), FileType.Project)
    projectFile.file.writeString("Project file content")

    // Create a local file.
    val localFile = FileEntity.createTemp("testLocalFile").copy(mimeType = Some("text/plain"))
    localFile.file.writeString("Local file content")

    // Create file entities
    implicit val pluginContext: PluginContext = PluginContext(Prefixes.empty, resources)
    val task = PlainTask("dummy", DatasetSpec(EmptyDataset))
    val fileEntities = FileEntitySchema.create(Seq(projectFile, localFile), task)

    // Convert to generic entities
    val genericEntities = GenericEntityTable(fileEntities.entities, fileEntities.entitySchema, task)

    // Convert back to file entities
    val fileEntitiesReconstructed = FileEntitySchema.unapply(genericEntities).get

    // Check result
    val firstFile = fileEntitiesReconstructed.typedEntities.next()
    firstFile.file.loadAsString() shouldBe "Project file content"
    firstFile.fileType shouldBe FileType.Project
    firstFile.mimeType shouldBe None

    val secondFile = fileEntitiesReconstructed.typedEntities.next()
    secondFile.file.loadAsString() shouldBe "Local file content"
    secondFile.fileType shouldBe FileType.Local
    secondFile.mimeType shouldBe Some("text/plain")
  }

  private def createTempResourceManager(): ResourceManager = {
    val tempDir = Files.createTempDirectory("testDirectory").toFile
    tempDir.deleteRecursiveOnExit()
    FileResourceManager(tempDir)
  }

}
