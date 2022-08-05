package org.silkframework.learning.active.poolgenerator
import org.silkframework.config.Prefixes
import org.silkframework.dataset.DataSource
import org.silkframework.learning.active.comparisons.ComparisonPair
import org.silkframework.learning.active.poolgenerator.LinkPoolGeneratorUtils._
import org.silkframework.learning.active.{LinkCandidate, UnlabeledLinkPool}
import org.silkframework.rule.LinkSpec
import org.silkframework.runtime.activity.{Activity, ActivityContext, UserContext}
import org.silkframework.util.DPair

import scala.collection.mutable

class CombinedLinkPoolGenerator(generators: LinkPoolGenerator*) extends LinkPoolGenerator {

  override def generator(inputs: DPair[DataSource], linkSpec: LinkSpec, paths: Seq[ComparisonPair], randomSeed: Long)
                        (implicit prefixes: Prefixes): Activity[UnlabeledLinkPool] = {
    val activities = generators.map(_.generator(inputs, linkSpec, paths, randomSeed))
    new LinkPoolGeneratorActivity(activities, linkSpec, paths)
  }

  private class LinkPoolGeneratorActivity(activities: Seq[Activity[UnlabeledLinkPool]], linkSpec: LinkSpec, paths: Seq[ComparisonPair]) extends Activity[UnlabeledLinkPool] {

    override def run(context: ActivityContext[UnlabeledLinkPool])(implicit userContext: UserContext): Unit = {
      val linkBuffer = mutable.Buffer[LinkCandidate]()
      val progressContribution = 1.0 /  activities.size

      for(activity <- activities) {
        val control = context.child(activity, progressContribution)
        linkBuffer ++= control.startBlockingAndGetValue().links
      }

      context.value() = UnlabeledLinkPool(entitySchema(linkSpec, paths), linkBuffer)
    }
  }
}
