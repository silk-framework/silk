package org.silkframework.learning.active.poolgenerator
import org.silkframework.dataset.DataSource
import org.silkframework.entity.Link
import org.silkframework.entity.paths.TypedPath
import org.silkframework.learning.active.UnlabeledLinkPool
import org.silkframework.rule.LinkSpec
import org.silkframework.runtime.activity.{Activity, ActivityContext, CombinedActivity, UserContext}
import org.silkframework.util.DPair
import LinkPoolGeneratorUtils._

import scala.collection.mutable

class CombinedLinkPoolGenerator(generators: LinkPoolGenerator*) extends LinkPoolGenerator {

  override def generator(inputs: DPair[DataSource], linkSpec: LinkSpec, paths: Seq[DPair[TypedPath]], randomSeed: Long): Activity[UnlabeledLinkPool] = {
    val activities = generators.map(_.generator(inputs, linkSpec, paths, randomSeed))
    new LinkPoolGeneratorActivity(activities, linkSpec, paths)
  }

  private class LinkPoolGeneratorActivity(activities: Seq[Activity[UnlabeledLinkPool]], linkSpec: LinkSpec, paths: Seq[DPair[TypedPath]]) extends Activity[UnlabeledLinkPool] {

    override def run(context: ActivityContext[UnlabeledLinkPool])(implicit userContext: UserContext): Unit = {
      val linkBuffer = mutable.Buffer[Link]()
      val progressContribution = 1.0 /  activities.size

      for(activity <- activities) {
        val control = context.child(activity, progressContribution)
        linkBuffer ++= control.startBlockingAndGetValue().links
      }

      context.value() = UnlabeledLinkPool(entitySchema(linkSpec, paths), linkBuffer.distinct)
    }
  }
}
