package de.fuberlin.wiwiss.silk.workbench.lift.comet

import de.fuberlin.wiwiss.silk.workbench.lift.util.ProgressWidget
import de.fuberlin.wiwiss.silk.workbench.evaluation.EvaluationServer

/**
 * Shows the progress of the evaluation task.
 */
class EvaluationProgress extends ProgressWidget(EvaluationServer.evaluationTask)
