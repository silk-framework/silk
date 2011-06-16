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
import de.fuberlin.wiwiss.silk.util.Timer
import de.fuberlin.wiwiss.silk.workbench.evaluation.EvalLink._

class ReferenceLinks extends LinkList
{
  private implicit val logger = Logger.getLogger(classOf[ReferenceLinks].getName)

  /** Register to updates to the ShowLinks variable */
  ShowLinks.subscribe(new Subscriber[UserData.ValueUpdated[EvalLink.Reference], Publisher[UserData.ValueUpdated[EvalLink.Reference]]]
  {
    def notify(pub : Publisher[UserData.ValueUpdated[EvalLink.Reference]], status : UserData.ValueUpdated[EvalLink.Reference])
    {
      partialUpdate(updateLinksCmd)
    }
  })

  override protected def links : Seq[EvalLink] =
  {
    val linkingTask = User().linkingTask
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
            new EvalLink(
              link = link,
              correct = if(link.confidence >= 0.0) Correct else Incorrect,
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
            new EvalLink(
              link = link,
              correct = if(link.confidence >= 0.0) Incorrect else Correct,
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
      case Positive if link.correct == Correct => <div>found</div>
      case Positive if link.correct == Incorrect => <div>not-found</div>
      case Negative if link.correct == Correct => <div>found</div>
      case Negative if link.correct == Incorrect => <div>not-found</div>
      case _ => <div>unknown</div>
    }
  }

  override protected def renderButtons(link : EvalLink)  : NodeSeq =
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
}