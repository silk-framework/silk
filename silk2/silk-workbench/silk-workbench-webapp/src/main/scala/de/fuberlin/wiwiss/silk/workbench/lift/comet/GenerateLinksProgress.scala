package de.fuberlin.wiwiss.silk.workbench.lift.comet

import de.fuberlin.wiwiss.silk.workbench.lift.util.ProgressWidget
import de.fuberlin.wiwiss.silk.workbench.evaluation.CurrentGenerateLinksTask
import de.fuberlin.wiwiss.silk.workbench.workspace.CurrentTaskStatusListener

/**
 * Shows the progress of the evaluation task.
 */
class GenerateLinksProgress extends ProgressWidget(new CurrentTaskStatusListener(CurrentGenerateLinksTask)) {
}
