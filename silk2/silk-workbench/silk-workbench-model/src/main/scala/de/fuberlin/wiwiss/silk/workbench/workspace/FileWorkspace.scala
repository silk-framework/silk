package de.fuberlin.wiwiss.silk.workbench.workspace

import java.io.File
import java.util.logging.Logger
import de.fuberlin.wiwiss.silk.util.FileUtils._

class FileWorkspace(file : File) extends Workspace
{
  private val logger = Logger.getLogger(classOf[FileWorkspace].getName)

  file.mkdir()

  private var projectList : List[Project] =
  {
    for(projectDir <- file.listFiles.filter(_.isDirectory).toList) yield
    {
      logger.info("Loading project: " + projectDir)
      new FileProject(projectDir)
    }
  }

  override def projects : List[Project] = projectList

  override def createProject(name : String) =
  {
    val projectDir = (file + ("/" + name))
    projectDir.mkdir()
    projectList ::= new FileProject(projectDir)
  }

  override def removeProject(name : String) =
  {
    (file + ("/" + name)).deleteRecursive()
    projectList = projectList.filterNot(_.name == name)
  }
}