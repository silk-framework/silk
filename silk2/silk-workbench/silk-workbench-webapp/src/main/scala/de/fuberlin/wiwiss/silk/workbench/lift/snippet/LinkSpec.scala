package de.fuberlin.wiwiss.silk.workbench.lift.snippet

import net.liftweb.util.Helpers._
import xml.NodeSeq
import java.io.StringReader
import de.fuberlin.wiwiss.silk.linkspec.LinkSpecification
import de.fuberlin.wiwiss.silk.workbench.workspace.User
import net.liftweb.http.js.{JsCmd, JsCmds}
import de.fuberlin.wiwiss.silk.workbench.lift.util.JS.Redirect
import java.util.logging.{Level, Logger}
import de.fuberlin.wiwiss.silk.util.CollectLogs
import net.liftweb.http.SHtml
import de.fuberlin.wiwiss.silk.workbench.lift.util.JS
import net.liftweb.http.js.JE.{Str, JsArray, JsRaw, Call}
import de.fuberlin.wiwiss.silk.evaluation.LinkConditionEvaluator
import de.fuberlin.wiwiss.silk.workbench.workspace.modules.linking.LinkingTask
import net.liftweb.http.js.JsCmds.{OnLoad, Script}

/**
 * LinkSpec snippet.
 *
 * Injects the 'linkSpec' variable which holds the current link spec.
 * Defines the 'reloadCache()' function which reloads the current cache containing the property paths.
 * Calls the serializeLinkSpec() function from the editor whenever the user saves the current link specification.
 */
class LinkSpec
{
  private val logger = Logger.getLogger(classOf[LinkSpec].getName)

  /**
   * Renders the toolbar.
   */
  def toolbar(xhtml : NodeSeq) : NodeSeq =
  {
    bind("entry", xhtml,
         "export" -> SHtml.ajaxButton("Export as Silk-LS", () => Redirect("config.xml")),
         "help" -> <a id="button" href="http://www.assembla.com/spaces/silk/wiki/Link_Specification_Editor" target="_help">Help</a>)
  }

  /**
   * Renders the content.
   */
  def content(xhtml : NodeSeq) : NodeSeq =
  {
    val updateLinkSpecFunction = JsCmds.Function("updateLinkSpec", "xml" :: Nil, SHtml.ajaxCall(JsRaw("xml"), updateLinkSpec _)._2.cmd)

    val initialStatus = OnLoad(Call("updateStatus", JsArray(), JsArray(), JsArray()).cmd)

    bind("entry", xhtml,
         "linkSpecVar" -> Script(linkSpecVarCmd & reloadCacheFunction & updateLinkSpecFunction & initialStatus))
  }

  /**
   * Updates the Link Specification.
   */
  private def updateLinkSpec(linkSpecStr : String) =
  {
    try
    {
      val project = User().project
      val linkingTask = User().linkingTask
      implicit val prefixes = project.config.prefixes

      //Collect warnings while saving link spec
      val warnings = CollectLogs(Level.WARNING)
      {
        //Load link specification
        val linkSpec = LinkSpecification.load(prefixes)(new StringReader(linkSpecStr))

        //Update linking task
        val updatedLinkingTask = linkingTask.copy(linkSpec = linkSpec)

        //Commit
        project.linkingModule.update(updatedLinkingTask)
        User().task = updatedLinkingTask
      }

      //Evaluate LinkSpec
      val infoMsg = evaluateLinkSpec(linkingTask)

      //Update link spec variable and notify user
      linkSpecVarCmd & Call("updateStatus", JsArray(), JsArray(warnings.map(_.getMessage).map(Str(_)).toList), JsArray(infoMsg.map(Str(_)).toList)).cmd
    }
    catch
    {
      case ex : Exception =>
      {
        logger.log(Level.INFO, "Failed to save link specification", ex)
        val msg = ex.getMessage
        //Strip prefixes like this: "cvc-complex-type.2.4.b:"
        val cleanMsg = if(msg.contains(':')) msg.split(':').tail.mkString else msg
        Call("updateStatus", JsArray(Str(cleanMsg)), JsArray(), JsArray()).cmd
      }
    }
  }

  private def evaluateLinkSpec(linkingTask : LinkingTask) : Option[String] =
  {
    if(linkingTask.cache.instances.positive.isEmpty || linkingTask.cache.instances.negative.isEmpty)
    {
      None
    }
    else
    {
      val r = LinkConditionEvaluator(linkingTask.linkSpec.condition, linkingTask.cache.instances)

      val msg = "Precision = " + r.precision + "\nRecall = " + r.recall + "\nF-measure = " + r.fMeasure

      Some(msg)
    }
  }

  /**
   * Command which sets the 'linkSpec' variable which holds the current link specification.
   */
  private def linkSpecVarCmd =
  {
    val linkingTask = User().linkingTask
    implicit val prefixes = User().project.config.prefixes

    //Serialize the link condition to a JavaScript string
    val linkSpecStr = linkingTask.linkSpec.toXML.toString.replace("\n", " ").replace("\\", "\\\\")

    val linkSpecVar = "var linkSpec = '" + linkSpecStr + "';"

    JsRaw(linkSpecVar).cmd
  }

  /**
   * JS Command which defines the reloadCache function
   */
  private def reloadCacheFunction : JsCmd =
  {
    def reloadCache =
    {
      User().linkingTask.cache.reload(User().project, User().linkingTask)
      JsRaw("").cmd
    }

    JsCmds.Function("reloadCache", Nil, SHtml.ajaxInvoke(reloadCache _)._2.cmd)
  }

//  private def generatePathsFunction() =
//  {
//    JsCmds.Function("retrievePaths", Nil, JsCmds.JsReturn(SHtml.ajaxInvoke(() => Str("test").cmd)._2))
//  }
//
//  private def generatePathsObj() =
//  {
//    new JsObj
//    {
//      val props = ("source", generateSelectedPathsObj(true)) ::
//                  ("target", generateSelectedPathsObj(false)) :: Nil
//    }.cmd
//  }
//
//  private def generateSelectedPathsObj(selectSource : Boolean) =
//  {
//    val dataset = Project().linkSpec.datasets.select(selectSource)
//
//    val instanceSpec = Project().cache.instanceSpecs.select(selectSource)
//
//    new JsObj
//    {
//      val props = ("id", Str(dataset.sourceId)) ::
//                  ("paths", JsArray(instanceSpec.paths.map(generatePathObj) : _*)) ::
//                  ("availablePaths", Num(instanceSpec.paths.size)) ::
//                  ("restrictions", Str(instanceSpec.restrictions)) :: Nil
//    }
//  }
//
//  private def generatePathObj(path : Path) =
//  {
//    new JsObj
//    {
//      val props = ("path", Str(path.toString)) ::
//                  ("frequency", Num(1.0)) :: Nil
//    }
//  }
}
