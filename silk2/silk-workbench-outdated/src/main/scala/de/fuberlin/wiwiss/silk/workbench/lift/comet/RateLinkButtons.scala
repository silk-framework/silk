/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.fuberlin.wiwiss.silk.workbench.lift.comet

import de.fuberlin.wiwiss.silk.workbench.evaluation.EvalLink
import xml.NodeSeq
import net.liftweb.http.SHtml
import net.liftweb.http.js.JsCmds._
import de.fuberlin.wiwiss.silk.workbench.evaluation.EvalLink.{Correct, Incorrect, Unknown}
import de.fuberlin.wiwiss.silk.workspace.User
import de.fuberlin.wiwiss.silk.entity.Link
import de.fuberlin.wiwiss.silk.evaluation.ReferenceLinks

/**
 * Adds buttons to rate a link to a link list.
 */
trait RateLinkButtons { self: LinksContent =>

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

  protected def confirmLink(link: Link) = {
    updateReferenceLinks(linkingTask.referenceLinks.withPositive(link))
    JsShowId(getId(link, "confirmedLink")) & JsHideId(getId(link, "declinedLink")) & JsHideId(getId(link, "undecidedLink"))
  }

  protected def declineLink(link: Link) = {
    updateReferenceLinks(linkingTask.referenceLinks.withNegative(link))
    JsShowId(getId(link, "declinedLink")) & JsHideId(getId(link, "confirmedLink")) & JsHideId(getId(link, "undecidedLink"))
  }

  protected def resetLink(link: Link) = {
    updateReferenceLinks(linkingTask.referenceLinks.without(link))
    JsShowId(getId(link, "undecidedLink")) & JsHideId(getId(link, "confirmedLink")) & JsHideId(getId(link, "declinedLink"))
  }

  protected def updateReferenceLinks(referenceLinks: ReferenceLinks) {
    val project = User().project
    val updatedTask = linkingTask.updateReferenceLinks(referenceLinks, project)

    project.linkingModule.update(updatedTask)
    User().task = updatedTask
  }
}