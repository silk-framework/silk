package de.fuberlin.wiwiss.silk.workbench.lift.comet

import collection.mutable.{Subscriber, Publisher}
import de.fuberlin.wiwiss.silk.workbench.evaluation._
import de.fuberlin.wiwiss.silk.output.Link
import de.fuberlin.wiwiss.silk.workbench.workspace.{User, UserData}
import de.fuberlin.wiwiss.silk.linkspec.evaluation.DetailedEvaluator
import net.liftweb.http.SHtml
import xml.NodeSeq
import net.liftweb.http.js.JsCmds.{OnLoad, SetHtml, Script, JsShowId, JsHideId}
import java.util.logging.Logger
import de.fuberlin.wiwiss.silk.workbench.evaluation.EvalLink._
import de.fuberlin.wiwiss.silk.util.Timer
import de.fuberlin.wiwiss.silk.util.task._

class ReferenceLinks extends LinkList
{
  private implicit val logger = Logger.getLogger(classOf[ReferenceLinks].getName)

  /** Minimum time in milliseconds between two successive updates*/
  private val minUpdatePeriod = 3000L

  /** The time of the last update */
  private var lastUpdateTime = 0L

  /** Register to updates to the ShowLinks variable */
  ShowLinks.subscribe(new Subscriber[UserData.ValueUpdated[EvalLink.ReferenceType], Publisher[UserData.ValueUpdated[EvalLink.ReferenceType]]]
  {
    def notify(pub : Publisher[UserData.ValueUpdated[EvalLink.ReferenceType]], status : UserData.ValueUpdated[EvalLink.ReferenceType])
    {
      partialUpdate(updateLinksCmd)
    }
  })

  /** Register to status messages of the cache loader task in order to be notified when new links are available */
  linkingTask.cache.loader.subscribe(new Subscriber[Status, Publisher[Status]]
  {
    def notify(pub : Publisher[Status], status : Status)
    {
      status match
      {
        case _ : Started =>
        {
        }
        case _ : Running if System.currentTimeMillis - lastUpdateTime > minUpdatePeriod =>
        {
          partialUpdate(updateLinksCmd)
          lastUpdateTime = System.currentTimeMillis
        }
        case _ : Finished =>
        {
          partialUpdate(updateLinksCmd)
        }
        case _ =>
      }
    }
  })

  override protected def links : Seq[EvalLink] =
  {
    def condition = linkingTask.linkSpec.condition
    def alignment = linkingTask.alignment
    def instances = linkingTask.cache.instances

    ShowLinks() match
    {
      case Positive =>
      {
        for(link <- alignment.positive.toSeq.view) yield instances.positive.get(link) match
        {
          case Some(instances) =>
          {
            val evaluatedLink = DetailedEvaluator(condition, instances, -1.0).get

            new EvalLink(
              link = evaluatedLink,
              correct = if(evaluatedLink.confidence >= 0.0) Correct else Incorrect,
              linkType = Positive
            )
          }
          case None =>
          {
            val cleanLink = new Link(link.source, link.target)

            new EvalLink(
              link = cleanLink,
              correct = Unknown,
              linkType = Positive
            )
          }
        }
      }
      case Negative =>
      {
        for(link <- alignment.negative.toSeq.view) yield instances.negative.get(link) match
        {
          case Some(instances) =>
          {
            val evaluatedLink = DetailedEvaluator(condition, instances, -1.0).get

            new EvalLink(
              link = evaluatedLink,
              correct = if(evaluatedLink.confidence >= 0.0) Incorrect else Correct,
              linkType = Negative
            )
          }
          case None =>
          {
            val cleanLink = new Link(link.source, link.target)

            new EvalLink(
              link = cleanLink,
              correct = Unknown,
              linkType = Negative
            )
          }
        }
      }
    }
  }

  override protected def renderStatus(link : EvalLink) : NodeSeq =
  {
    ShowLinks() match
    {
      case Positive if link.correct == Correct => <div>correct</div>
      case Positive if link.correct == Incorrect => <div>incorrect</div>
      case Negative if link.correct == Correct => <div>correct</div>
      case Negative if link.correct == Incorrect => <div>incorrect</div>
      case _ => <div>unknown</div>
    }
  }

  override protected def renderButtons(link : EvalLink) : NodeSeq =
  {
    <div>
      {SHtml.a(() => resetLink(link), <img src="./static/img/delete.png" />)}
    </div>
  }

  private def resetLink(link : Link) =
  {
    val alignment = linkingTask.alignment
    val updatedTask = linkingTask.copy(alignment = alignment.copy(positive = alignment.positive - link, negative = alignment.negative - link))

    User().project.linkingModule.update(updatedTask)
    User().task = updatedTask

    JsHideId(getId(link))
  }
}