package org.silkframework.runtime.resource

import org.scalatest.FlatSpec
import org.silkframework.runtime.resource.WritableResource.WritableResourceException
import org.silkframework.util.ConfigTestTrait

import java.nio.file.Files
import org.silkframework.util.FileUtils._

class FileResourceFreeSpaceThresholdTest extends FlatSpec with ConfigTestTrait {
  private val tempDir = Files.createTempDirectory("doesntmatter").toFile
  tempDir.deleteRecursiveOnExit()
  private val currentFreeSpace = tempDir.getFreeSpace

  it should "prevent writing a new file when not enough space is available" in {
    val rm = new FileResourceManager(tempDir.getAbsolutePath)
    assertThrows[WritableResourceException]{
      rm.get("file").writeString("...")
    }
  }

  it should "allow writing files when enough space is available" in {
    val rm = FileResourceManager(tempDir, Some(1L))
    rm.get("file").writeString("...")
  }

  override def propertyMap: Map[String, Option[String]] = {
    Map(
      // Require more free space than available
      WritableResource.fileSystemFreeSpaceThresholdKey -> Some(s"${currentFreeSpace * 2}")
    )
  }
}
