package models.linking

import de.fuberlin.wiwiss.silk.entity.Link
import de.fuberlin.wiwiss.silk.execution.GenerateLinks
import de.fuberlin.wiwiss.silk.runtime.activity.{Activity, ActivityControl, ValueHolder}
import models.TaskData

object GenerateLinksActivity {

  private lazy val activity = Activity.empty[Seq[Link]]

  def apply() = activity
}

object CurrentGenerateLinksTask extends TaskData[ActivityControl[_]](null) {

}

object CurrentGeneratedLinks extends TaskData(new ValueHolder[Seq[Link]]()) {

}

