package org.silkframework.runtime.resource

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

import java.io.File

class FileResourceManagerTest extends AnyFlatSpec with Matchers {
  behavior of "file resource manager"

  it should "not allow access outside of its base path" in {
    val tempDir = File.createTempFile("prefix", "").getParentFile
    val tempDirName = tempDir.getName
    val manager = FileResourceManager(tempDir)

    // get method
    manager.get("someDir/../allowedAccess")
    manager.get(s"someDir/../../$tempDirName/allowedAccess")
    intercept[IllegalArgumentException] {
      manager.get("../../etc/passwd")
    }
    intercept[IllegalArgumentException] {
      manager.get("somePath/morePath/./../../../etc/passwd")
    }

    // child method
    manager.child("someDir/../allowedAccess")
    manager.child(s"someDir/../../$tempDirName/allowedAccess")
    intercept[IllegalArgumentException] {
      manager.child("../../etc/passwd")
    }
    intercept[IllegalArgumentException] {
      manager.child("somePath/morePath/./../../../etc/passwd")
    }
  }
}
