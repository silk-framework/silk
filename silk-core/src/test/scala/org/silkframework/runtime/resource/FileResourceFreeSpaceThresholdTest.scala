package org.silkframework.runtime.resource

import org.scalatest.BeforeAndAfterAll
import org.silkframework.config.DefaultConfig
import org.silkframework.util.FileUtils._

import java.nio.file.Files
import org.scalatest.flatspec.AnyFlatSpec

class FileResourceFreeSpaceThresholdTest extends AnyFlatSpec with BeforeAndAfterAll {
  private val tempDir = Files.createTempDirectory("doesntmatter").toFile
  tempDir.deleteRecursiveOnExit()
  private val currentFreeSpace = tempDir.getFreeSpace

  it should "prevent writing a new file when not enough space is available" in {
    System.setProperty(Resource.freeSpaceThresholdParameterName, s"${currentFreeSpace * 2}")
    DefaultConfig.instance.refresh()
    val rm = new FileResourceManager(tempDir.getAbsolutePath)
    assertThrows[NotEnoughDiskSpaceException]{
      rm.get("file").writeString("...")
    }
  }

  it should "allow writing files when enough space is available" in {
    System.setProperty(Resource.freeSpaceThresholdParameterName, s"1")
    DefaultConfig.instance.refresh()
    val rm = FileResourceManager(tempDir)
    rm.get("file").writeString("...")
  }

  override def afterAll(): Unit = {
    System.clearProperty(Resource.freeSpaceThresholdParameterName)
    DefaultConfig.instance.refresh()
    super.afterAll()
  }
}
