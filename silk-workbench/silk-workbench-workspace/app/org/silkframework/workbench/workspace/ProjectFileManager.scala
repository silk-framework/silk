package org.silkframework.workbench.workspace

import java.util.UUID

import play.api.libs.Files.TemporaryFile

object ProjectFileManager {

  private var files: Map[UUID, TemporaryFile] = Map.empty

  def addFile(file: TemporaryFile): UUID = {
    val uuid = UUID.randomUUID()
    files += (uuid -> file)
    uuid
  }

  def deleteFile(uuid: UUID): Unit = {
    files(uuid).delete()
    files -= uuid
  }

}
