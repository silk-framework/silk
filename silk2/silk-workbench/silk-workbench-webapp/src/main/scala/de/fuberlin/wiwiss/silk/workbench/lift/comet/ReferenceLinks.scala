package de.fuberlin.wiwiss.silk.workbench.lift.comet

import collection.mutable.{Subscriber, Publisher}
import de.fuberlin.wiwiss.silk.workbench.evaluation._
import de.fuberlin.wiwiss.silk.output.Link
import de.fuberlin.wiwiss.silk.workbench.workspace.{User, UserData}
import de.fuberlin.wiwiss.silk.linkspec.evaluation.DetailedEvaluator
import net.liftweb.http.SHtml
import xml.NodeSeq
import net.liftweb.http.js.JsCmds.{OnLoad, SetHtml, Script, JsShowId, JsHideId}

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

            (new ReferenceLink(evaluatedLink, true),  correct)
          }
          case None => (new ReferenceLink(link, true), if(link.confidence >= threshold) 1 else -1)
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

            (new ReferenceLink(evaluatedLink, false),  correct)
          }
          case None => (new ReferenceLink(link, false), if(link.confidence >= threshold) -1 else 1)
        }
      }
    }
  }

  override protected def renderStatus(link : Link, correct : Int) : NodeSeq =
  {
    ShowLinks() match
    {
      case PositiveLinks if correct ==  1 => <div>found</div>
      case PositiveLinks if correct == -1 => <div>not-found</div>
      case NegativeLinks if correct ==  1 => <div>found</div>
      case NegativeLinks if correct == -1 => <div>not-found</div>
      case _ => <div>unknown</div>
    }
  }

  override protected def renderButtons(link : Link, correct : Int)  : NodeSeq =
  {
    <div>
      {SHtml.a(() => resetLink(link), <img src="./static/img/delete.png" />)}
    </div>
  }

  private def resetLink(link : Link) =
  {
    val linkingTask = User().linkingTask
    val alignment = linkingTask.alignment
    val updatedTask = linkingTask.copy(alignment = alignment.copy(positive = alignment.positive - link, negative = alignment.negative - link))

    User().project.linkingModule.update(updatedTask)
    User().task = updatedTask

    JsHideId(getId(link))
  }

  private class ReferenceLink(link : Link, val isPositive : Boolean) extends Link(link.sourceUri, link.targetUri, link.confidence, link.details)
}