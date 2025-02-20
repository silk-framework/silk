package org.silkframework.dataset.operations

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.config.Prefixes
import org.silkframework.dataset.operations.DeleteFilesOperatorTest.createResourceManager
import org.silkframework.runtime.plugin.{ClassPluginDescription, ParameterValues, PluginContext}
import org.silkframework.runtime.resource.{FileResourceManager, ResourceManager}
import org.silkframework.util.FileUtils

import java.nio.file.Files

class DeleteFilesOperatorTest extends AnyFlatSpec with Matchers {

  it should "provide a method to do a dry run" in {
    testDryRun(
      regex = "subdir.*",
      existingFiles = Seq("another", "subdir/file.csv", "subdirFile.csv", "this")
    ) should contain theSameElementsAs Seq("subdir/file.csv", "subdirFile.csv")
  }

  it should "provide a dry run action that can be called using the Plugin API" in {
    val result = testDryRunPlugin(
      regex = "file1.csv",
      existingFiles = Seq("file1.csv", "file2.csv")
    )
    result should include ("file1.csv")
    result should not include ("file2.csv")
  }

  def testDryRun(regex: String, existingFiles: Seq[String]): Seq[String] = {
    val deleteFiles = DeleteFilesOperator(regex)
    val resourceManager = createResourceManager(existingFiles)
    implicit val pluginContext: PluginContext = PluginContext(Prefixes.empty, resourceManager)
    deleteFiles.dryRun.filesToDelete
  }

  def testDryRunPlugin(regex: String, existingFiles: Seq[String]): String = {
    val resourceManager = createResourceManager(existingFiles)
    implicit val pluginContext: PluginContext = PluginContext(Prefixes.empty, resourceManager)

    val pluginDesc = ClassPluginDescription(classOf[DeleteFilesOperator])
    pluginDesc.actions.size shouldBe 1
    pluginDesc.actions.head.label shouldBe "Dry run"

    val plugin = pluginDesc(ParameterValues.fromStringMap(Map("filesRegex" -> regex)))
    pluginDesc.actions.head.call(plugin)
  }

}

object DeleteFilesOperatorTest extends Matchers {

  def createResourceManager(files: Seq[String]): ResourceManager = {
    val tempDir = Files.createTempDirectory("test-resource-manager-dir").toFile
    FileUtils.toFileUtils(tempDir).deleteRecursiveOnExit()
    val manager = FileResourceManager(tempDir)
    for(file <- files) {
      manager.get(file).writeString("content")
    }
    manager
  }

}