import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

    val appName         = "SilkWorkbench"
    val appVersion      = "2.5.4"

    val appDependencies = Seq(
      "de.fuberlin.wiwiss.silk" % "silk-workspace" % "2.5.4"
    )

    val main = play.Project(appName, appVersion, appDependencies).settings(
      resolvers += ("Local Maven Repository" at "file:"+Path.userHome+"/.m2/repository")
    )

}
