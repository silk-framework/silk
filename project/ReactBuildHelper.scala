import org.apache.commons.io.FileUtils

import java.io.{File, FileInputStream}
import java.util.Properties
import java.util.logging.Logger
import java.util.regex.Pattern
import scala.annotation.tailrec
import scala.sys.process.{BasicIO, Process, ProcessLogger}

object ReactBuildHelper {
  val log: Logger = Logger.getLogger(this.getClass.getCanonicalName)
  final val WAIT_AFTER_PROCESS_ERROR_MS = 1000 // 1 second
  final val MAX_RETRIES_YARN_DEPENDENCY_RESOLUTION = 5
  /**
    * The Yarn command to execute, needs to be found on Windows first.
    */
  lazy val yarnCommand: String = sys.props.get("os.name") match {
    case Some(os) if os.toLowerCase.contains("win") =>
      // On windows, we need to provide the path of the yarn script manually
      process("where.exe" :: "yarnpkg.cmd" :: Nil).trim.takeWhile(c => c != '\n' && c != '\r')
    case _ =>
      "yarnpkg"
  }

  // Config file for the Silk UI build that extends the default build
  val silkBuildPropertiesFile = "silk-ui-build.properties"

  @tailrec
  def searchConfigFileRecursively(currentDir: File): Option[File] = {
    val potentialConfigFile = new File(currentDir, silkBuildPropertiesFile)
    if(potentialConfigFile.exists() && potentialConfigFile.isFile && potentialConfigFile.canRead) {
      log.info(s"Found '$silkBuildPropertiesFile' in directory '${currentDir.getAbsolutePath}'.")
      Some(potentialConfigFile)
    } else if(potentialConfigFile.getParentFile != null) {
      searchConfigFileRecursively(currentDir.getParentFile)
    } else {
      None
    }
  }

  /** Fetch build properties if they exist. */
  def buildConfig(reactBuildRoot: File): Properties = {
    // Look for build configuration recursively
    val properties = new Properties()
    searchConfigFileRecursively(reactBuildRoot).foreach(f => properties.load(new FileInputStream(f)))
    properties
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
    assert(missing.isEmpty, "Following required command line tools are missing: yarn")
  }

  /**
    * Builds React components and copies the generated files.
    *
    * @param reactBuildRoot          The root directory of the React build, i.e. where the package.json is located etc.
    * @param targetArtifactDirectory The directory where the built artifacts are copied to. This directory is deleted
    *                                prior to copying the files.
    * @param project                 The name of the project to build, for logging and documentation.
    */
  def buildReactComponentsAndCopy(reactBuildRoot: File, targetArtifactDirectory: File, project: String): Unit = BasicIO.synchronized {
    val buildEnv = sys.env.getOrElse("BUILD_ENV", "development")
    val productionBuild = buildEnv == "production"
    val buildTask = if (productionBuild) "webpack-build" else "webpack-dev-build"
    log.info(s"Building $project React components for $buildEnv, running task $buildTask...")
    yarnInstall(reactBuildRoot)

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
    log.info(s"Finished building '$project' React components.")
  }

  /**
    * Builds React components.
    *
    * @param reactBuildRoot          The root directory of the React build, i.e. where the package.json is located etc.
    * @param project                 The name of the project to build, for logging and documentation.
    */
  def buildReactComponents(reactBuildRoot: File, project: String): Unit = BasicIO.synchronized {
    val buildEnv = sys.env.getOrElse("BUILD_ENV", "development")
    val productionBuild = buildEnv == "production"
    val buildTask = if (productionBuild) "build-di-prod" else "build-di-dev"
    log.info(s"Building $project React UI for $buildEnv, running task $buildTask...")
    yarnInstall(reactBuildRoot)

    process(yarnCommand :: buildTask :: Nil, reactBuildRoot) // Build main artifact

    log.info(s"Finished building '$project' React UI.")
  }

  @tailrec
  private def findSilkRoot(currentDir: File): Option[File] = {
    if (isSilkRoot(currentDir)) {
      Some(currentDir)
    } else {
      if (currentDir.getParentFile != null) {
        findSilkRoot(currentDir.getParentFile)
      } else {
        None
      }
    }
  }

  private def isSilkRoot(dir: File): Boolean = {
    val packageJson = new File(dir, "package.json")
    if(packageJson.exists() && packageJson.isFile && packageJson.canRead) {
      val source = scala.io.Source.fromFile(packageJson)
      val packageJsonContent = source.getLines().mkString("")
      source.close()
      packageJsonContent.matches(".*\"name\"\\s*:\\s*\"silk\"\\s*,.*")
    } else {
      false
    }
  }

  /** Install dependencies and setup yarn workspaces. */
  def yarnInstall(reactBuildRoot: File): Unit = BasicIO.synchronized {
    log.info("Setting up yarn workspaces environment...")
    // Either bootstrap in Silk or in the directory the build config file has been found
    searchConfigFileRecursively(reactBuildRoot)
      .map(_.getParentFile)
      .orElse(findSilkRoot(reactBuildRoot)) match {
      case Some(realProjectRoot) =>
        process(yarnCommand :: "--frozen-lockfile" :: Nil, realProjectRoot, maxRetries = MAX_RETRIES_YARN_DEPENDENCY_RESOLUTION) // Install dependencies
      case None =>
        throw new RuntimeException(s"Directory '${reactBuildRoot.getAbsolutePath}' is neither inside the Silk repository nor" +
          s" nested in a directory containing a build config file '$silkBuildPropertiesFile'.")
    }
  }

  def process(command: Seq[String], workingDir: File, maxRetries: Int = 0): String = BasicIO.synchronized {
    var tries = 0
    while(tries <= maxRetries) {
      val (out, err) = (new StringBuffer(), new StringBuffer())
      val logger = ProcessLogger(
        line => out.append(line).append("\n"),
        line => err.append(line).append("\n")
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
          log.warning(s"Executing process ${command.mkString(" ")} has failed. Output before failure:\n$out\nError output:\n$err")
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
    if(Watcher.staleTargetFiles(WatchConfig(sourceFile.getParentFile, Pattern.quote(sourceFile.getName)), Seq(targetFile))) {
      println("Transpiling (ES5) " + sourceFile.getCanonicalPath + " to " + targetFile.getCanonicalPath)
      process(yarnCommand :: "babel" :: sourceFile.getCanonicalPath :: s"--out-file=${targetFile.getCanonicalPath}" :: Nil, reactBuildRoot)
    }
  }
}