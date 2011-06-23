package de.fuberlin.wiwiss.silk.workbench.workspace

import java.io.File
import java.util.logging.Logger
import de.fuberlin.wiwiss.silk.util.FileUtils._
import de.fuberlin.wiwiss.silk.util.Identifier

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

  override def createProject(name : Identifier) =
  {
    require(!projectList.exists(_.name == name), "A project with the name '" + name + "' already exists")

    val projectDir = (file + ("/" + name))
    projectDir.mkdir()
    val newProject = new FileProject(projectDir)
    projectList ::= newProject
    newProject
  }

  override def removeProject(name : Identifier) =
  {
    (file + ("/" + name)).deleteRecursive()
    projectList = projectList.filterNot(_.name == name)
  }
}