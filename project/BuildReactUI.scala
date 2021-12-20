import java.io.File
import java.util.logging.Logger

import ReactBuildHelper.process

import scala.sys.process.BasicIO

/**
  * Helper methods to build the DI React UI components from an sbt build script.
  */
object BuildReactUI {
  val log: Logger = Logger.getLogger(this.getClass.getCanonicalName)
  final val WAIT_AFTER_PROCESS_ERROR_MS = 1000 // 1 second
  final val MAX_RETRIES_YARN_DEPENDENCY_RESOLUTION = 5

  private val yarnCommand = ReactBuildHelper.yarnCommand


  /**
    * Builds React components and copies the generated files.
    *
    * @param reactBuildRoot          The root directory of the React build, i.e. where the package.json is located etc.
    */
  def buildReactComponents(reactBuildRoot: File, project: String): Unit = BasicIO.synchronized {
    val buildEnv = sys.env.getOrElse("BUILD_ENV", "development")
    val productionBuild = buildEnv == "production"
    val buildTask = if (productionBuild) "build-di-prod" else "build-di-dev"
    log.info(s"Building $project React UI for $buildEnv, running task $buildTask...")

    process(yarnCommand :: Nil, reactBuildRoot, maxRetries = MAX_RETRIES_YARN_DEPENDENCY_RESOLUTION) // Install dependencies
    process(yarnCommand :: buildTask :: Nil, reactBuildRoot) // Build main artifact

    log.info(s"Finished building $project React UI.")
  }
}
