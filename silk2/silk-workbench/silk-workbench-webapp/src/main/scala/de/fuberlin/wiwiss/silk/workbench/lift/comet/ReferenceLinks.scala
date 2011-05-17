package de.fuberlin.wiwiss.silk.workbench.lift.comet

import collection.mutable.{Subscriber, Publisher}
import de.fuberlin.wiwiss.silk.workbench.evaluation._
import de.fuberlin.wiwiss.silk.output.Link
import de.fuberlin.wiwiss.silk.workbench.workspace.{User, UserData}
import de.fuberlin.wiwiss.silk.linkspec.evaluation.DetailedEvaluator

class ReferenceLinks extends LinkList
{
  /** Register to updates to the ShowLinks variable */
  ShowLinks.subscribe(new Subscriber[UserData.ValueUpdated[LinkType], Publisher[UserData.ValueUpdated[LinkType]]]
  {
    def notify(pub : Publisher[UserData.ValueUpdated[LinkType]], status : UserData.ValueUpdated[LinkType])
    {
      partialUpdate(updateLinks)
    }
  })

  override protected def linkCount : Int =
  {
    ShowLinks() match
    {
      case PositiveLinks => User().linkingTask.alignment.positive.size
      case NegativeLinks => User().linkingTask.alignment.negative.size
    }
  }

  override protected def links(from : Int, until : Int) : Traversable[(Link, Int)] =
  {
    val linkingTask = User().linkingTask
    def condition = linkingTask.linkSpec.condition
    def threshold = linkingTask.linkSpec.filter.threshold
    def alignment = linkingTask.alignment
    def instances = linkingTask.cache.instances

    ShowLinks() match
    {
      case PositiveLinks =>
      {
        for(link <- alignment.positive.view(from, until)) yield instances.positive.get(link) match
        {
          case Some(instances) =>
          {
            val evaluatedLink = DetailedEvaluator(condition, instances, 0.0).get
            val correct = if(evaluatedLink.confidence >= threshold) 1 else -1

            (evaluatedLink,  correct)
          }
          case None => (link, if(link.confidence >= threshold) 1 else -1)
        }
      }
      case NegativeLinks =>
      {
        for(link <- alignment.negative.view(from, until)) yield instances.negative.get(link) match
        {
          case Some(instances) =>
          {
            val evaluatedLink = DetailedEvaluator(condition, instances, 0.0).get
            val correct = if(evaluatedLink.confidence >= threshold) -1 else 1

            (evaluatedLink,  correct)
          }
          case None => (link, if(link.confidence >= threshold) -1 else 1)
        }
      }
    }
  }
}