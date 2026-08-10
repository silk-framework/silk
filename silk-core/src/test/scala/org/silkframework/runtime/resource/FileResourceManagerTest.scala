package org.silkframework.runtime.resource

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

import java.io.File
import java.nio.file.Files

class FileResourceManagerTest extends AnyFlatSpec with Matchers {
  behavior of "file resource manager"

  /** Creates a fresh base directory with a sibling directory that shares its name prefix. */
  private def withBaseAndSiblingDir(test: (File, File) => Unit): Unit = {
    val root = Files.createTempDirectory("fileResourceManagerTest").toFile
    val baseDir = new File(root, "resources")
    val siblingDir = new File(root, "resources-backup")
    baseDir.mkdir()
    siblingDir.mkdir()
    try {
      test(baseDir, siblingDir)
    } finally {
      def deleteRecursive(file: File): Unit = {
        if (file.isDirectory) Option(file.listFiles).foreach(_.foreach(deleteRecursive))
        file.delete()
      }
      deleteRecursive(root)
    }
  }

  it should "not allow access outside of its base path" in {
    val tempDir = File.createTempFile("prefix", "").getParentFile
    val tempDirName = tempDir.getName
    val manager = FileResourceManager(tempDir)

    // get method
    manager.get("someDir/../allowedAccess")
    manager.get(s"someDir/../../$tempDirName/allowedAccess")
    intercept[ResourceAccessDeniedException] {
      manager.get("../../etc/passwd")
    }
    intercept[ResourceAccessDeniedException] {
      manager.get("somePath/morePath/./../../../etc/passwd")
    }

    // child method
    manager.child("someDir/../allowedAccess")
    manager.child(s"someDir/../../$tempDirName/allowedAccess")
    intercept[ResourceAccessDeniedException] {
      manager.child("../../etc/passwd")
    }
    intercept[ResourceAccessDeniedException] {
      manager.child("somePath/morePath/./../../../etc/passwd")
    }
  }

  it should "not allow deleting outside of its base path" in {
    withBaseAndSiblingDir { (baseDir, siblingDir) =>
      val manager = FileResourceManager(baseDir)
      val ownFile = new File(baseDir, "own.csv")
      val siblingFile = new File(siblingDir, "secret.csv")
      Files.writeString(ownFile.toPath, "own")
      Files.writeString(siblingFile.toPath, "secret")

      // Deleting the parent of the base directory must not be possible
      intercept[ResourceAccessDeniedException] {
        manager.delete("..")
      }
      // Neither via a nested path that climbs back out
      intercept[ResourceAccessDeniedException] {
        manager.delete("nested/../../resources-backup")
      }
      // Nor the base directory itself
      intercept[ResourceAccessDeniedException] {
        manager.delete("")
      }
      baseDir must exist
      siblingDir must exist
      siblingFile must exist

      // Deleting a file inside the base directory still works
      manager.delete("own.csv")
      ownFile mustNot exist
    }
  }

  it should "not allow access to a sibling directory that shares the base directory name prefix" in {
    withBaseAndSiblingDir { (baseDir, siblingDir) =>
      val manager = FileResourceManager(baseDir)
      val siblingFile = new File(siblingDir, "secret.csv")
      Files.writeString(siblingFile.toPath, "secret")

      // '../resources-backup' passes a plain string prefix check, but must be rejected
      intercept[ResourceAccessDeniedException] {
        manager.get("../resources-backup/secret.csv")
      }
      intercept[ResourceAccessDeniedException] {
        manager.child("../resources-backup")
      }
      intercept[ResourceAccessDeniedException] {
        manager.delete("../resources-backup/secret.csv")
      }
      siblingFile must exist
    }
  }
}
