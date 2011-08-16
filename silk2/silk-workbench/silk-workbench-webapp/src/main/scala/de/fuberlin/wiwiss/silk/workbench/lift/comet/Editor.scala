package de.fuberlin.wiwiss.silk.workbench.lift.comet

import java.io.StringReader
import de.fuberlin.wiwiss.silk.linkspec.LinkSpecification
import de.fuberlin.wiwiss.silk.workbench.workspace.User
import net.liftweb.http.js.{JsCmd, JsCmds}
import java.util.logging.{Level, Logger}
import de.fuberlin.wiwiss.silk.evaluation.LinkConditionEvaluator
import de.fuberlin.wiwiss.silk.workbench.workspace.modules.linking.LinkingTask
import xml.Text
import net.liftweb.http.js.JsCmds.{JsCrVar, OnLoad, Script}
import de.fuberlin.wiwiss.silk.util.ValidationException.ValidationError
import net.liftweb.http.js.JE._
import de.fuberlin.wiwiss.silk.util.{ValidationException, CollectLogs}
import net.liftweb.http.{CometActor, SHtml}
import collection.mutable.{Publisher, Subscriber}
import de.fuberlin.wiwiss.silk.util.task.{Finished, Status}

/**
 * Link specification editor.
 *
 * Injects the 'linkSpec' variable which holds the current link spec.
 * Defines the 'reloadCache()' function which reloads the current cache containing the property paths.
 * Calls the serializeLinkSpec() function from the editor whenever the user saves the current link specification.
 */
class Editor extends CometActor {
  private val logger = Logger.getLogger(classOf[Editor].getName)

  /** Redraw the widget on every view. */
  override protected val dontCacheRendering = true

  //Listen to status messages of the cache loader task
  User().linkingTask.cache.subscribe(CacheListener)

  /**
   * Renders the editor.
   */
  override def render = {
    val updateLinkSpecFunction = JsCmds.Function("updateLinkSpec", "xml" :: Nil, SHtml.ajaxCall(JsRaw("xml"), updateLinkSpec _)._2.cmd)

    val initialStatus = OnLoad(updateStatusCall(infos = evaluateLinkSpec(User().linkingTask)))

    bind("entry", defaultHtml,
         "linkSpecVar" -> Script(linkSpecVarCmd & reloadCacheFunction & updateLinkSpecFunction & initialStatus))
  }

  /**
   * Updates the Link Specification.
   */
  private def updateLinkSpec(linkSpecStr: String) = {
    try {
      val project = User().project
      val linkingTask = User().linkingTask
      implicit val prefixes = project.config.prefixes

      //Collect warnings while saving link spec
      val warnings = CollectLogs(Level.WARNING, "de.fuberlin.wiwiss.silk.linkspec") {
        //Load link specification
        val linkSpec = LinkSpecification.load(prefixes)(new StringReader(linkSpecStr))

        //Update linking task
        val updatedLinkingTask = linkingTask.updateLinkSpec(linkSpec, project)

        //Listen to status messages of the cache loader task
        updatedLinkingTask.cache.subscribe(CacheListener)

        //Commit
        project.linkingModule.update(updatedLinkingTask)
        User().task = updatedLinkingTask
      }

      //Update link spec variable and notify user
      linkSpecVarCmd & updateStatusCall(warnings = warnings.map(_.getMessage), infos = evaluateLinkSpec(User().linkingTask))
    } catch {
      case ex: ValidationException => {
        logger.log(Level.INFO, "Cannot save invalid link specification", ex)
        updateStatusCall(errors = ex.errors)
      }
      case ex: Exception => {
        logger.log(Level.INFO, "Failed to save link specification", ex)
        updateStatusCall(errors = ValidationError("Error in back end: " + ex.getMessage) :: Nil)
      }
    }
  }

  private def updateStatusCall(errors: Traversable[ValidationError] = Traversable.empty,
                               warnings: Traversable[String] = Traversable.empty,
                               infos: Traversable[String] = Traversable.empty) = {
    /**Generates a JavaScript expression from a string */
    def toJsExp(str: String) = Str(Text(str).toString)

    /**Generates a JavaScript array from a collection of strings*/
    def toJsArray(messages: Traversable[String]) = JsArray(messages.map(toJsExp).toList)

    /**Generates a JavaScript expression from an error */
    def errorToJsExp(error: ValidationError) = JsObj(("message", Str(error.message)), ("id", Str(error.id.map(_.toString).getOrElse(""))))

    /**Generates a JavaScript array from a collection of errors*/
    def errorsToJsArray(messages: Traversable[ValidationError]) = JsArray(messages.map(errorToJsExp).toList)

    /**Create a call to the update status function */
    Call("updateStatus", errorsToJsArray(errors), toJsArray(warnings), toJsArray(infos)).cmd
  }

  private def evaluateLinkSpec(linkingTask: LinkingTask): Traversable[String] = {
    if(linkingTask.cache.isRunning) {
      ("Cache loading") :: Nil
    } else if(linkingTask.cache.failed) {
      ("Cache loading failed") :: Nil
    } else if (linkingTask.cache.instances.positive.isEmpty || linkingTask.cache.instances.negative.isEmpty) {
      ("No reference links") :: Nil
    } else {
      val r = LinkConditionEvaluator(linkingTask.linkSpec.condition, linkingTask.cache.instances)

      ("Precision = %.2f".format(r.precision)) ::
      ("Recall = %.2f".format(r.recall)) ::
      ("F-measure = %.2f".format(r.fMeasure)) :: Nil
    }
  }

  /**
   * Command which sets the 'linkSpec' variable which holds the current link specification.
   */
  private def linkSpecVarCmd = {
    val linkingTask = User().linkingTask
    implicit val prefixes = User().project.config.prefixes

    //Serialize the link condition to a JavaScript string
    val linkSpecStr = linkingTask.linkSpec.toXML.toString.replace("\n", " ")

    JsCrVar("linkSpec", Str(linkSpecStr))
  }

  /**
   * JS Command which defines the reloadCache function
   */
  private def reloadCacheFunction: JsCmd = {
    def reloadCache = {
      User().linkingTask.cache.reload(User().project, User().linkingTask.linkSpec, User().linkingTask.alignment)
      JsRaw("").cmd
    }

    JsCmds.Function("reloadCache", Nil, SHtml.ajaxInvoke(reloadCache _)._2.cmd)
  }

  /**
   * Updates the status as soon as the cache has been loaded.
   */
  object CacheListener extends Subscriber[Status, Publisher[Status]] {
    def notify(pub : Publisher[Status], status : Status) {
      status match {
        case _ : Finished => {
          partialUpdate(updateStatusCall(infos = evaluateLinkSpec(User().linkingTask)))
        }
        case _ =>
      }
    }
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
