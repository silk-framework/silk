package de.fuberlin.wiwiss.silk.workbench.lift.comet

import de.fuberlin.wiwiss.silk.workbench.lift.util.ProgressWidget
import de.fuberlin.wiwiss.silk.workbench.workspace.User
import collection.mutable.{Publisher, Subscriber}
import de.fuberlin.wiwiss.silk.workbench.workspace.User.CurrentTaskChanged

/**
 * Shows the progress of the cache loader task.
 */
class CacheLoadingProgress extends ProgressWidget(User().linkingTask.cache, hide = true) {
  /** Register to messages of the user. */
  User().onUpdate(CurrentTaskListener)

  private object CurrentTaskListener extends (CurrentTaskChanged => Unit) {
    def apply(event: CurrentTaskChanged) {
      reRender()
    }
  }
}