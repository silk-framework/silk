package models.linking

import de.fuberlin.wiwiss.silk.entity.Link
import de.fuberlin.wiwiss.silk.execution.GenerateLinks
import de.fuberlin.wiwiss.silk.runtime.activity.{ActivityControl, ValueHolder}
import models.TaskData

object CurrentGenerateLinksTask extends TaskData[ActivityControl[_]](null) {

}

object CurrentGeneratedLinks extends TaskData(new ValueHolder[Seq[Link]]()) {

}

