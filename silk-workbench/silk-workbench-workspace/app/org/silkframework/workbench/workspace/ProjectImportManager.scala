package org.silkframework.workbench.workspace

import java.util.UUID

import org.silkframework.runtime.activity.{Activity, ActivityControl, Status}
import org.silkframework.workspace.ProjectMarshallingTrait
import play.api.libs.Files.TemporaryFile

object ProjectImportManager {

  private var files: Map[String, TemporaryFile] = Map.empty
  private var marshallers: Map[String, ProjectMarshallingTrait] = Map.empty

  private var importActivities: Map[String, ActivityControl[Unit]] = Map.empty

  def addFile(file: TemporaryFile, marshaller: ProjectMarshallingTrait): String = {
    val uuid = UUID.randomUUID().toString
    files += (uuid -> file)
    marshallers += (uuid -> marshaller)
    uuid
  }

  def deleteFile(uuid: String): Unit = {
    files(uuid).delete()
    files -= uuid
    marshallers -= uuid
  }

  def startImport(uuid: String, projectLabel: String): Unit = {
    val file = files(uuid)
    val marshaller = marshallers(uuid)
    val control = Activity(new ProjectImportActivity(projectLabel, file, marshaller))
    importActivities += (uuid -> control)
  }

  def getImportStatus(uuid: String): Option[Status] = {
    importActivities.get(uuid).map(_.status())
  }

}
