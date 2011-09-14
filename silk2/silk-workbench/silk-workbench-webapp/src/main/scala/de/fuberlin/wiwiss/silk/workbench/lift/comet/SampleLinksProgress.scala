package de.fuberlin.wiwiss.silk.workbench.lift.comet

import de.fuberlin.wiwiss.silk.workbench.lift.util.ProgressWidget
import de.fuberlin.wiwiss.silk.workbench.workspace.CurrentTaskStatusListener
import de.fuberlin.wiwiss.silk.workbench.learning.CurrentSampleLinksTask

class SampleLinksProgress extends ProgressWidget(new CurrentTaskStatusListener(CurrentSampleLinksTask)) {
}