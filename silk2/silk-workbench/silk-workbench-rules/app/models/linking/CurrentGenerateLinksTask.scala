package models.linking

import de.fuberlin.wiwiss.silk.entity.Link
import de.fuberlin.wiwiss.silk.execution.GenerateLinksTask
import de.fuberlin.wiwiss.silk.runtime.task.{TaskControl, ValueHolder}
import models.TaskData

object CurrentGenerateLinksTask extends TaskData[TaskControl](null) {

}

object CurrentGeneratedLinks extends TaskData(new ValueHolder[Seq[Link]](Seq.empty)) {

}

