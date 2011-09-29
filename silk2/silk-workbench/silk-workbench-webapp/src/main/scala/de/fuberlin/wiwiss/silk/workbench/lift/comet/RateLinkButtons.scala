package de.fuberlin.wiwiss.silk.workbench.lift.comet

import de.fuberlin.wiwiss.silk.workbench.evaluation.EvalLink
import xml.NodeSeq
import net.liftweb.http.SHtml
import de.fuberlin.wiwiss.silk.output.Link
import net.liftweb.http.js.JsCmds._
import de.fuberlin.wiwiss.silk.workbench.evaluation.EvalLink.{Correct, Incorrect, Unknown}
import de.fuberlin.wiwiss.silk.workbench.workspace.User

/**
 * Adds buttons to rate a link to a link list.
 */
trait RateLinkButtons { self: Links =>

  override protected def renderButtons(link: EvalLink): NodeSeq = {
    <div id={getId(link, "confirmedLink")} style={if(link.correct == Correct) "display:block" else "display:none"}>
      <a><img src="./static/img/confirm.png" /></a>
      {SHtml.a(() => resetLink(link), <img src="./static/img/undecided-disabled.png" />)}
      {SHtml.a(() => declineLink(link), <img src="./static/img/decline-disabled.png" />)}
    </div>
    <div id={getId(link, "declinedLink")} style={if(link.correct == Incorrect) "display:block" else "display:none"}>
      {SHtml.a(() => confirmLink(link), <img src="./static/img/confirm-disabled.png" />)}
      {SHtml.a(() => resetLink(link), <img src="./static/img/undecided-disabled.png" />)}
       <a><img src="./static/img/decline.png" /></a>
    </div>
    <div id={getId(link, "undecidedLink")} style={if(link.correct == Unknown) "display:block" else "display:none"}>
      {SHtml.a(() => confirmLink(link), <img src="./static/img/confirm-disabled.png" />)}
       <a><img src="./static/img/undecided.png" /></a>
      {SHtml.a(() => declineLink(link), <img src="./static/img/decline-disabled.png" />)}
    </div>
  }

  private def confirmLink(link: Link) = {
    val project = User().project
    val referenceLinks = linkingTask.referenceLinks
    val updatedTask = linkingTask.updateReferenceLinks(referenceLinks.withPositive(link), project)

    project.linkingModule.update(updatedTask)
    User().task = updatedTask

    JsShowId(getId(link, "confirmedLink")) & JsHideId(getId(link, "declinedLink")) & JsHideId(getId(link, "undecidedLink"))
  }

  private def declineLink(link: Link) = {
    val project = User().project
    val referenceLinks = linkingTask.referenceLinks
    val updatedTask = linkingTask.updateReferenceLinks(referenceLinks.withNegative(link), project)

    project.linkingModule.update(updatedTask)
    User().task = updatedTask

    JsShowId(getId(link, "declinedLink")) & JsHideId(getId(link, "confirmedLink")) & JsHideId(getId(link, "undecidedLink"))
  }

  private def resetLink(link: Link) = {
    val project = User().project
    val referenceLinks = linkingTask.referenceLinks
    val updatedTask = linkingTask.updateReferenceLinks(referenceLinks.without(link), project)

    project.linkingModule.update(updatedTask)
    User().task = updatedTask

    JsShowId(getId(link, "undecidedLink")) & JsHideId(getId(link, "confirmedLink")) & JsHideId(getId(link, "declinedLink"))
  }
}