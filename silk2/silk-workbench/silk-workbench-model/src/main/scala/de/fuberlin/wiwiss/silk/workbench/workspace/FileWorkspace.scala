package de.fuberlin.wiwiss.silk.workbench.workspace

import java.io.File
import java.util.logging.Logger

class FileWorkspace (file : File) extends Workspace
{
   private val logger = Logger.getLogger(classOf[FileWorkspace].getName)

   file.mkdir()

   override def projects : List[Project] =
    {  logger.info("-----------------")
      for(projectDir <- file.listFiles.filter(_.isDirectory).toList) yield
      {

        logger.info("Loading project: " + projectDir)
        new FileProject(projectDir)

      }
    }
  
}