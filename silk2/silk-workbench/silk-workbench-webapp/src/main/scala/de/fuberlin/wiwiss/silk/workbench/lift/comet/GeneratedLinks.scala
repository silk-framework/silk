package de.fuberlin.wiwiss.silk.workbench.lift.comet

import collection.mutable.{Publisher, Subscriber}
import de.fuberlin.wiwiss.silk.util.Task
import de.fuberlin.wiwiss.silk.workbench.workspace.User
import de.fuberlin.wiwiss.silk.output.Link
import net.liftweb.http.SHtml
import xml.NodeSeq
import de.fuberlin.wiwiss.silk.workbench.workspace.UserData._
import de.fuberlin.wiwiss.silk.workbench.evaluation.{NegativeLinks, PositiveLinks}
import net.liftweb.http.js.JsCmds.{OnLoad, SetHtml, Script, JsShowId, JsHideId}

class GeneratedLinks extends LinkList
{
  /** Minimum time in milliseconds between two successive updates*/
  private val minUpdatePeriod = 3000L

  /** The time of the last update */
  private var lastUpdateTime = 0L

  override protected val showStatus = false

  /** Register to status messages of the evaluation task in order to be notified when new links are available */
  User().evaluationTask.subscribe(new Subscriber[Task.StatusMessage, Publisher[Task.StatusMessage]]
  {
    def notify(pub : Publisher[Task.StatusMessage], status : Task.StatusMessage)
    {
      status match
      {
        case Task.Started() =>
        {
        }
        case Task.StatusChanged(_, _) if System.currentTimeMillis - lastUpdateTime > minUpdatePeriod =>
        {
          partialUpdate(updateLinks)
          lastUpdateTime = System.currentTimeMillis
        }
        case Task.Finished(_, _) =>
        {
          partialUpdate(updateLinks)
        }
        case _ =>
      }
    }
  })

  override protected def linkCount : Int =
  {
    User().evaluationTask.links.size
  }

  override protected def links(from : Int, until : Int) : Traversable[(Link, Int)] =
  {
    val linkingTask = User().linkingTask
    def alignment = linkingTask.alignment

    for(link <- User().evaluationTask.links.view(from, until)) yield
    {
      if(alignment.positive.contains(link))
      {
        (link, 1)
      }
      else if(alignment.negative.contains(link))
      {
        (link, -1)
      }
      else
      {
        (link, 0)
      }
    }
  }

  override protected def renderStatus(link : Link, correct : Int) : NodeSeq =
  {
    correct match
    {
      case 1 => <div>correct</div>
      case -1 => <div>wrong</div>
      case _ => <div>unknown</div>
    }
  }

  override protected def renderButtons(link : Link, correct : Int) : NodeSeq =
  {
    <div id={getId(link, "confirmedLink")} style={if(correct == 1) "display:block" else "display:none"}>
      <a><img src="./static/img/confirm.png" /></a>
      {SHtml.a(() => resetLink(link), <img src="./static/img/undecided-disabled.png" />)}
      {SHtml.a(() => declineLink(link), <img src="./static/img/decline-disabled.png" />)}
    </div>
    <div id={getId(link, "declinedLink")} style={if(correct == -1) "display:block" else "display:none"}>
      {SHtml.a(() => confirmLink(link), <img src="./static/img/confirm-disabled.png" />)}
      {SHtml.a(() => resetLink(link), <img src="./static/img/undecided-disabled.png" />)}
       <a><img src="./static/img/decline.png" /></a>
    </div>
    <div id={getId(link, "undecidedLink")} style={if(correct == 0) "display:block" else "display:none"}>
      {SHtml.a(() => confirmLink(link), <img src="./static/img/confirm-disabled.png" />)}
       <a><img src="./static/img/undecided.png" /></a>
      {SHtml.a(() => declineLink(link), <img src="./static/img/decline-disabled.png" />)}
    </div>
  }

  private def confirmLink(link : Link) =
  {
    val linkingTask = User().linkingTask
    val alignment = linkingTask.alignment
    val updatedTask = linkingTask.copy(alignment = alignment.copy(positive = alignment.positive + link, negative = alignment.negative - link))

    User().project.linkingModule.update(updatedTask)
    User().task = updatedTask

    JsShowId(getId(link, "confirmedLink")) & JsHideId(getId(link, "declinedLink")) & JsHideId(getId(link, "undecidedLink"))
  }

  private def declineLink(link : Link) =
  {
    val linkingTask = User().linkingTask
    val alignment = linkingTask.alignment
    val updatedTask = linkingTask.copy(alignment = alignment.copy(positive = alignment.positive - link, negative = alignment.negative + link))

    User().project.linkingModule.update(updatedTask)
    User().task = updatedTask

    JsShowId(getId(link, "declinedLink")) & JsHideId(getId(link, "confirmedLink")) & JsHideId(getId(link, "undecidedLink"))
  }

  private def resetLink(link : Link) =
  {
    val linkingTask = User().linkingTask
    val alignment = linkingTask.alignment
    val updatedTask = linkingTask.copy(alignment = alignment.copy(positive = alignment.positive - link, negative = alignment.negative - link))

    User().project.linkingModule.update(updatedTask)
    User().task = updatedTask

    JsShowId(getId(link, "undecidedLink")) & JsHideId(getId(link, "confirmedLink")) & JsHideId(getId(link, "declinedLink"))
  }
}
