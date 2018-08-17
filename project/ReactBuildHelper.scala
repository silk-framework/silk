import java.io.File
import java.util.logging.Logger

import org.apache.commons.io.FileUtils

import scala.sys.process.{BasicIO, Process, ProcessLogger}

object ReactBuildHelper {
  val log: Logger = Logger.getLogger(this.getClass.getCanonicalName)
  final val WAIT_AFTER_PROCESS_ERROR_MS = 1000 // 1 second
  final val MAX_RETRIES_YARN_DEPENDENCY_RESOLUTION = 5
  /**
    * The Yarn command to execute, needs to be found on Windows first.
    */
  val yarnCommand: String = sys.props.get("os.name") match {
    case Some(os) if os.toLowerCase.contains("win") =>
      // On windows, we need to provide the path of the yarn script manually
      process("where.exe" :: "yarn.cmd" :: Nil).trim
    case _ =>
      "yarn"
  }

  /**
    * Checks if some common build tools are available on this system.
    */
  def checkReactBuildTool(): Unit = BasicIO.synchronized {
    val missing = Seq(yarnCommand) filter { name =>
      scala.util.Try {
        process(name :: "--version" :: Nil) == ""
      } getOrElse true
    }

    missing foreach { m =>
      println(s"Command line tool $m is missing for building JavaScript artifacts!")
    }
    assert(missing.isEmpty, "Required command line tools are missing")
  }

  /**
    * Builds React components and copies the generated files.
    *
    * @param reactBuildRoot          The root directory of the React build, i.e. where the package.json is located etc.
    * @param targetArtifactDirectory The directory where the built artifacts are copied to. This directory is deleted
    *                                prior to copying the files.
    */
  def buildReactComponents(reactBuildRoot: File, targetArtifactDirectory: File, project: String): Unit = BasicIO.synchronized {
    val buildEnv = sys.env.getOrElse("BUILD_ENV", "development")
    val productionBuild = buildEnv == "production"
    val buildTask = if (productionBuild) "webpack-build" else "webpack-dev-build"
    log.info(s"Building $project React components for $buildEnv, running task $buildTask...")

    process(yarnCommand :: Nil, reactBuildRoot, maxRetries = MAX_RETRIES_YARN_DEPENDENCY_RESOLUTION) // Install dependencies
    // Run build via gulp task, this has been the old way of building it
    //          Process("yarn" :: "run" :: "deploy" :: Nil, reactBuildRoot).!! // Build main artifact

    // Run build via webpack only, uncomment source map copy instruction when using this
    process(yarnCommand :: buildTask :: Nil, reactBuildRoot) // Build main artifact

    FileUtils.deleteDirectory(targetArtifactDirectory)
    FileUtils.forceMkdir(targetArtifactDirectory)
    val files = Seq( // React components build artifacts
      new File(reactBuildRoot, "dist/main.js"),
      new File(reactBuildRoot, "dist/main.js.map"),
      new File(reactBuildRoot, "dist/style.css"),
      new File(reactBuildRoot, "dist/style.css.map")
    ).filter(f => !f.getName.endsWith(".map") || productionBuild)
    for (file <- files) {
      FileUtils.copyFileToDirectory(file, targetArtifactDirectory)
    }
    FileUtils.copyDirectoryToDirectory(new File(reactBuildRoot, "dist/fonts"), targetArtifactDirectory)
    log.info(s"Finished building $project React components.")
  }

  private def process(command: Seq[String], workingDir: File, maxRetries: Int = 0): String = {
    var tries = 0
    while(tries <= maxRetries) {
      val (out, err) = (new StringBuffer(), new StringBuffer())
      val logger = ProcessLogger(
        out.append(_),
        err.append(_)
      )
      val proc = Process(command, workingDir)
      val exitCode = proc.!(logger)
      if (exitCode == 0) {
        return out.toString
      } else {
        val errorMessage = s"Executing external process '${command.mkString(" ")}' in working directory " +
            s"${workingDir.getCanonicalPath} failed with error code " + exitCode +
            s" and error output: ${err.toString}"
        if(tries == maxRetries) {
          throw new Error(errorMessage)
        } else {
          log.warning(errorMessage + "\nRetrying execution...")
          Thread.sleep(WAIT_AFTER_PROCESS_ERROR_MS)
        }
      }
      tries += 1
    }
    throw new Error("Not executed!")
  }

  def process(command: Seq[String]): String = {
    process(command, new File("./"))
  }

  /**
    * Transpiles JavaScript Code to ES5 JavaScript Code.
    *
    * @param reactBuildRoot The root directory of the JavaScript build pipeline, i.e. where the package.json resides.
    * @param sourceFile     Source JS file
    * @param targetFile     Target transpiled JS file
    */
  def transpileJavaScript(reactBuildRoot: File, sourceFile: File, targetFile: File): Unit = {
    FileUtils.forceMkdir(targetFile.getParentFile)
    println("Transpiling (ES5) " + sourceFile.getCanonicalPath + " to " + targetFile.getCanonicalPath)
    process(yarnCommand :: "babel" :: sourceFile.getCanonicalPath :: s"--out-file=${targetFile.getCanonicalPath}" :: Nil, reactBuildRoot)
  }
}