package de.fuberlin.wiwiss.silk.workbench.lift.comet

import de.fuberlin.wiwiss.silk.workbench.lift.util.ProgressWidget
import de.fuberlin.wiwiss.silk.workbench.evaluation.CurrentGenerateLinksTask
import de.fuberlin.wiwiss.silk.workbench.workspace.CurrentTaskStatusListener

/**
 * Shows the progress of the evaluation task.
 */
class LinkGenerationProgress extends ProgressWidget(new CurrentTaskStatusListener(CurrentGenerateLinksTask)) {
}
