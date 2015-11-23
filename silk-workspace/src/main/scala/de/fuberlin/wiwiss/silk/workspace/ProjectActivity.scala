package de.fuberlin.wiwiss.silk.workspace

import de.fuberlin.wiwiss.silk.runtime.activity.{Activity, ActivityContext}

/**
  * An activity that belongs to a project.
  *
  * @param project The project this activity belongs to
  * @param activityGenerator A function that generates a new activity for a project.
  */
class ProjectActivity(override val name: String, project: Project, activityGenerator: Project => Activity[Unit]) extends Activity[Unit] {

  /**
    * Executes this activity.
    *
    * @param context Holds the context in which the activity is executed.
    */
  override def run(context: ActivityContext[Unit]): Unit = {
    val activity = activityGenerator(project)
    activity.run(context)
  }
}
