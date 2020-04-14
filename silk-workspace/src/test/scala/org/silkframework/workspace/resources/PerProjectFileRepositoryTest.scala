package org.silkframework.workspace.resources

import java.nio.file.Files
import org.silkframework.util.FileUtils._

class PerProjectFileRepositoryTest extends PerProjectResourceRepositoryTest {

  private val tempFile = Files.createTempDirectory("SharedFileRepositoryTest").toFile
  tempFile.deleteRecursiveOnExit()

  override protected lazy val repository: ResourceRepository = PerProjectFileRepository(tempFile.getAbsolutePath)
}
