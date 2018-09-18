package org.silkframework.runtime.resource

import java.io.File

import org.scalatest.{FlatSpec, MustMatchers}

class FileResourceManagerTest extends FlatSpec with MustMatchers {
  behavior of "file resource manager"

  it should "not allow access below its base path" in {
    val tempDir = File.createTempFile("prefix", "").getParentFile
    val tempDirName = tempDir.getName
    val manager = FileResourceManager(tempDir)
    manager.get("someDir/../allowedAccess")
    manager.get(s"someDir/../../$tempDirName/allowedAccess")
    intercept[IllegalArgumentException] {
      manager.get("../../etc/passwd")
    }
    intercept[IllegalArgumentException] {
      manager.get("somePath/morePath/./../../../etc/passwd")
    }
  }
}
