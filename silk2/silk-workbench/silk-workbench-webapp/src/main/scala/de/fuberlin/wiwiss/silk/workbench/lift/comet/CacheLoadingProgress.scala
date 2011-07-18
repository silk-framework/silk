package de.fuberlin.wiwiss.silk.workbench.lift.comet

import de.fuberlin.wiwiss.silk.workbench.lift.util.ProgressWidget
import de.fuberlin.wiwiss.silk.workbench.workspace.User


/**
 * Shows the progress of the cache loader task.
 */
class CacheLoadingProgress extends ProgressWidget(User().linkingTask.cache, true)