import org.apache.commons.io.FileUtils
import java.io.File
import sbt.Process

object ReactBuildHelper {
  /**
    * The Yarn command to execute, needs to be found on Windows first.
    */
  val yarnCommand: String = sys.props.get("os.name") match {
    case Some(os) if os.toLowerCase.contains("win") =>
      // On windows, we need to provide the path of the yarn script manually
      Process("where.exe" :: "yarn.cmd" :: Nil).!!.trim
    case _ =>
      "yarn"
  }

  /**
    * Checks if some common build tools are available on this system.
    */
  def checkReactBuildTool(): Unit = {
    val missing = Seq(yarnCommand) filter { name =>
      scala.util.Try {
        Process(name :: "--version" :: Nil).!! == ""
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
  def buildReactComponents(reactBuildRoot: File, targetArtifactDirectory: File, project: String): Unit = {
    val buildEnv = sys.env.getOrElse("BUILD_ENV", "development")
    val productionBuild = buildEnv == "production"
    val buildTask = if (productionBuild) "webpack-build" else "webpack-dev-build"
    println(s"Building $project React components for $buildEnv, running task $buildTask...")

    Process(yarnCommand :: Nil, reactBuildRoot).!! // Install dependencies
    // Run build via gulp task, this has been the old way of building it
    //          Process("yarn" :: "run" :: "deploy" :: Nil, reactBuildRoot).!! // Build main artifact

    // Run build via webpack only, uncomment source map copy instruction when using this
    Process(yarnCommand :: buildTask :: Nil, reactBuildRoot).!! // Build main artifact

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
    println("Finished building React components.")
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
    Process(yarnCommand :: "babel" :: sourceFile.getCanonicalPath :: s"--out-file=${targetFile.getCanonicalPath}" :: Nil, reactBuildRoot).!!
  }
}