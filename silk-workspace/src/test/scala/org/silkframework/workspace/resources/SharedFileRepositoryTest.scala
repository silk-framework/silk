package org.silkframework.workspace.resources

import java.nio.file.Files
import org.silkframework.util.FileUtils._

class SharedFileRepositoryTest extends SharedResourceRepositoryTest {

  private val tempFile = Files.createTempDirectory("SharedFileRepositoryTest").toFile
  tempFile.deleteRecursiveOnExit()

  override protected lazy val repository: ResourceRepository = SharedFileRepository(tempFile.getAbsolutePath)
}
