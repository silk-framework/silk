package org.silkframework.runtime.resource

import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.silkframework.util.FileUtils._
import org.silkframework.util.TestFileUtils

import java.io.File
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger

class FileResourceManagerTest extends AnyFlatSpec with ResourceManagerTestTrait with BeforeAndAfterAll {

  private val root = TestFileUtils.createTempDirectoryForTest("fileResourceManagerTest", "")

  private val dirCounter = new AtomicInteger()

  /** Each test gets its own base directory below the temporary root. */
  override protected def createResourceManager(): ResourceManager = {
    val baseDir = new File(root, "base" + dirCounter.incrementAndGet())
    baseDir.mkdir()
    FileResourceManager(baseDir)
  }

  override def afterAll(): Unit = {
    root.deleteRecursive()
    super.afterAll()
  }

  /** Creates a fresh base directory with a sibling directory that shares its name prefix. */
  private def withBaseAndSiblingDir(test: (File, File) => Unit): Unit = {
    val dir = new File(root, "sibling" + dirCounter.incrementAndGet())
    val baseDir = new File(dir, "resources")
    val siblingDir = new File(dir, "resources-backup")
    baseDir.mkdirs()
    siblingDir.mkdir()
    test(baseDir, siblingDir)
  }

  behavior of "File resource manager"

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
      baseDir should exist
      siblingDir should exist
      siblingFile should exist

      // Deleting a file inside the base directory still works
      manager.delete("own.csv")
      ownFile shouldNot exist
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
      siblingFile should exist
    }
  }
}
