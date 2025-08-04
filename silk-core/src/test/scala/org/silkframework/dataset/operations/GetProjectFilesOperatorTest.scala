package org.silkframework.dataset.operations

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.config.Prefixes
import org.silkframework.dataset.operations.DeleteFilesOperatorTest.createResourceManager
import org.silkframework.runtime.plugin.PluginContext

class GetProjectFilesOperatorTest extends AnyFlatSpec with Matchers {

  behavior of "Get project files operator"

  it should "allow to retrieve files by name" in {
    test(
      op = GetProjectFilesOperator("subdir/file.csv", ""),
      existingFiles = Seq("another", "subdir/file.csv", "subdirFile.csv", "this")
    ) should contain theSameElementsAs Seq("subdir/file.csv")
  }

  it should "allow to retrieve files by regex" in {
    test(
      op = GetProjectFilesOperator("", "subdir.*"),
      existingFiles = Seq("another", "subdir/file.csv", "subdirFile.csv", "this")
    ) should contain theSameElementsAs Seq("subdir/file.csv", "subdirFile.csv")
  }

  def test(op: GetProjectFilesOperator, existingFiles: Seq[String]): Seq[String] = {
    val resourceManager = createResourceManager(existingFiles)
    implicit val pluginContext: PluginContext = PluginContext(Prefixes.empty, resourceManager)
    op.dryRun.files
  }

}