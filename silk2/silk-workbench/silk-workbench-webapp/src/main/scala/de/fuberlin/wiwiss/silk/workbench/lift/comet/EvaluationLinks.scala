package de.fuberlin.wiwiss.silk.workbench.lift.comet

import collection.mutable.{Publisher, Subscriber}
import de.fuberlin.wiwiss.silk.util.Task
import net.liftweb.http.{SHtml, CometActor}
import net.liftweb.http.js.{JsCmd, JsCmds}
import net.liftweb.http.js.JE.JsRaw
import de.fuberlin.wiwiss.silk.output.Link
import net.liftweb.http.js.JsCmds.{OnLoad, SetHtml, Script, JsShowId, JsHideId}
import de.fuberlin.wiwiss.silk.workbench.workspace.{UserData, User}
import de.fuberlin.wiwiss.silk.linkspec.evaluation.DetailedEvaluator
import xml.{Text, NodeSeq}
import de.fuberlin.wiwiss.silk.workbench.evaluation._

/**
* A widget which displays the generated links of the evaluation server.
*/
class EvaluationLinks extends CometActor
{
  /** The number of links shown on one page */
  private val pageSize = 100

  /** Minimum time in milliseconds between two successive updates*/
  private val minUpdatePeriod = 3000L

  /** The time of the last update */
  private var lastUpdateTime = 0L

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

  /** Register to updates to the ShowLinks variable */
  ShowLinks.subscribe(new Subscriber[UserData.ValueUpdated[LinkType], Publisher[UserData.ValueUpdated[LinkType]]]
  {
    def notify(pub : Publisher[UserData.ValueUpdated[LinkType]], status : UserData.ValueUpdated[LinkType])
    {
      partialUpdate(updateLinks)
    }
  })

  override def render =
  {
    val showLinksFunc = JsCmds.Function("showLinks", "page" :: Nil, SHtml.ajaxCall(JsRaw("page"), (pageStr) => showLinks(pageStr.toInt))._2.cmd)

    <p>
      { Script(OnLoad(updateLinks) & showLinksFunc) }
      { <div id="results" /> }
    </p>
  }

  private def updateLinks() : JsCmd =
  {
    JsRaw("initPagination(" + linkCount + ");").cmd
  }

  private def showLinks(page : Int) =
  {
    val html =
      <div>
        <div class="link">
          <div class="link-header heading">
            <div class="source-link"><span>Source</span></div>
            <div class="target-link"><span>Target</span></div>
            <div><span>Confidence</span></div>
          </div>
        </div>
        {
          for((link, correct) <- links(page * pageSize, (page + 1) * pageSize)) yield
          {
            renderLink(link, correct)
          }
        }
      </div>

    SetHtml("results", html) & JsRaw("initTrees();").cmd
  }

  /**
   * Renders a link.
   *
   * @param link The link to be rendered
   * @param correct 1 if this link is correct. -1 if it is wrong. 0 if unknown.
   */
  private def renderLink(link : Link, correct : Int) =
  {
    <div class="link">
      <div class="link-header" onclick={"toggleLinkDetails('" + getId(link) + "');"} onmouseover="$(this).addClass('link-over');" onmouseout="$(this).removeClass('link-over');">
        <div id={getId(link, "toggle")}><span class="ui-icon ui-icon ui-icon-triangle-1-e"></span></div>
        <div class="source-link"><a href={link.sourceUri} target="_blank">{link.sourceUri}</a></div>
        <div class="target-link"><a href={link.targetUri} target="_blank">{link.targetUri}</a></div>
        <div class="confidencebar"><div class="confidence">{"%.1f".format(link.confidence * 100)}%</div></div>
        <div class="link-buttons">
          <div id={getId(link, "unknownLink")} style={if(correct == 0) "display:block" else "display:none"}>
          {SHtml.a(() => declineLink(link), <img src="./static/img/decline.png" />)}
          {SHtml.a(() => confirmLink(link), <img src="./static/img/confirm.png" />)}
          </div>
          <div id={getId(link, "confirmedLink")} style={if(correct == 1) "display:block" else "display:none"}>
          {SHtml.a(() => resetLink(link), <img src="./static/img/correct.png" />)}
          </div>
          <div id={getId(link, "declinedLink")} style={if(correct == -1) "display:block" else "display:none"}>
          {SHtml.a(() => resetLink(link), <img src="./static/img/uncorrect.png" />)}
          </div>
        </div>
      </div>
      <div class="link-details" id={getId(link, "details")}>
      { renderDetails(link.details) }
      </div>
      <div style="clear:both"></div>
    </div>
  }

  private def renderDetails(details : Option[Link.Similarity]) : NodeSeq =
  {
    details match
    {
      case Some(similarity) =>
      {
        <ul class="details-tree">
        { renderSimilarity(similarity) }
        </ul>
      }
      case None => Text("No details")
    }
  }

  private def renderSimilarity(similarity : Link.Similarity) : NodeSeq = similarity match
  {
    case Link.AggregatorSimilarity(value, children) =>
    {
      <li>
        <span class="aggregation">Aggregation</span>{ renderConfidence(value) }
          <ul>
            { children.map(renderSimilarity) }
          </ul>
      </li>
    }
    case Link.ComparisonSimilarity(value, input1, input2) =>
    {
      <li>
        <span class="comparison">Comparison</span>{ renderConfidence(value) }
          <ul>
            { renderInputValue(input1) }
            { renderInputValue(input2) }
          </ul>
      </li>
    }
  }
  private def renderConfidence(value : Option[Double]) = value match
  {
    case Some(v) => <div class="confidencebar"><div class="confidence">{"%.1f".format(v * 100)}%</div></div>
    case None => NodeSeq.Empty
  }

  private def renderInputValue(input : Link.InputValue) = input match
  {
    case Link.InputValue(path, values) =>
    {
      <li><span class="input">{ path.toString + ": " + values.mkString(", ") }</span></li>
    }
  }

  private def confirmLink(link : Link) =
  {
    val linkingTask = User().linkingTask
    val alignment = linkingTask.alignment
    val updatedTask = linkingTask.copy(alignment = alignment.copy(positive = alignment.positive + link))

    User().project.linkingModule.update(updatedTask)
    User().task = updatedTask

    JsShowId(getId(link, "confirmedLink")) & JsHideId(getId(link, "unknownLink"))
  }

  private def declineLink(link : Link) =
  {
    val linkingTask = User().linkingTask
    val alignment = linkingTask.alignment
    val updatedTask = linkingTask.copy(alignment = alignment.copy(negative = alignment.negative + link))

    User().project.linkingModule.update(updatedTask)
    User().task = updatedTask

    JsShowId(getId(link, "declinedLink")) & JsHideId(getId(link, "unknownLink"))
  }

  private def resetLink(link : Link) =
  {
    val linkingTask = User().linkingTask
    val alignment = linkingTask.alignment
    val updatedTask = linkingTask.copy(alignment = alignment.copy(positive = alignment.positive - link, negative = alignment.negative - link))

    User().project.linkingModule.update(updatedTask)
    User().task = updatedTask

    JsShowId(getId(link, "unknownLink")) & JsHideId(getId(link, "confirmedLink")) & JsHideId(getId(link, "declinedLink"))
  }

  private def getId(link : Link, prefix : String = "") =
  {
    prefix + link.hashCode
  }

  private def linkCount : Int =
  {
    ShowLinks() match
    {
      case GeneratedLinks => User().evaluationTask.links.size
      case PositiveLinks => User().linkingTask.alignment.positive.size
      case NegativeLinks => User().linkingTask.alignment.negative.size
    }
  }

  private def links(from : Int, until : Int) : Traversable[(Link, Int)] =
  {
    val linkingTask = User().linkingTask
    def condition = linkingTask.linkSpec.condition
    def threshold = linkingTask.linkSpec.filter.threshold
    def alignment = linkingTask.alignment
    def instances = linkingTask.cache.instances

    ShowLinks() match
    {
      case GeneratedLinks =>
      {
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
