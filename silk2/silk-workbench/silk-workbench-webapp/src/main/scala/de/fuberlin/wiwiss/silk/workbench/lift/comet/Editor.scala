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

import de.fuberlin.wiwiss.silk.config.LinkSpecification
import de.fuberlin.wiwiss.silk.workbench.workspace.User
import net.liftweb.http.js.{JsCmd, JsCmds}
import java.util.logging.{Level, Logger}
import de.fuberlin.wiwiss.silk.evaluation.LinkageRuleEvaluator
import de.fuberlin.wiwiss.silk.workbench.workspace.modules.linking.LinkingTask
import xml.Text
import net.liftweb.http.js.JsCmds.{JsCrVar, OnLoad, Script}
import de.fuberlin.wiwiss.silk.util.ValidationException.ValidationError
import net.liftweb.http.js.JE._
import de.fuberlin.wiwiss.silk.util.{ValidationException, CollectLogs}
import net.liftweb.http.{CometActor, SHtml}
import de.fuberlin.wiwiss.silk.util.task.{TaskFinished, TaskStatus}
import de.fuberlin.wiwiss.silk.workbench.workspace.io.SilkConfigExporter
import java.io.StringReader
import java.util.Properties
import java.io.{FileNotFoundException, FileReader, File}

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
  User().linkingTask.cache.onUpdate(CacheListener)

  private var currentInfos = Traversable[String]()
  private var currentWarnings = Traversable[String]()
  private var currentErrors = Traversable[ValidationError]()

  private var repositories:Map[String, Map[String, String]] = Map()

  try {
    val configFile = new File("./config.properties");

    val properties = new Properties()
    properties.load(new FileReader(configFile))

    if(properties.getProperty("linkSpecRepository") != null) {
      val repositoryString = properties.getProperty("linkSpecRepository")
      val repositoriesNames = repositoryString.split(" ")
      repositoriesNames.foreach(
        repository => {
          val repositoryUrl = properties.getProperty("linkSpecRepository."+repository+".URL")
          val repositoryApiKey = properties.getProperty("linkSpecRepository."+repository+".API_Key")
          if ((repositoryUrl != null) && (repositoryApiKey != null)) {
            repositories += repository -> Map("url" -> repositoryUrl, "apiKey" -> repositoryApiKey)
          }
        }
      )
    }
  } catch {
    case _ : FileNotFoundException =>
      {
      }
  }

  /**
   * Renders the editor.
   */
  override def render = {
    val updateLinkSpecFunction = JsCmds.Function("updateLinkSpec", "xml" :: Nil, SHtml.ajaxCall(JsRaw("xml"), updateLinkSpec _)._2.cmd)

    val initialStatus = OnLoad(updateStatusCall(errors = Traversable.empty, warnings = Traversable.empty, infos = evaluateLinkSpec(User().linkingTask)))

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
        updatedLinkingTask.cache.onUpdate(CacheListener)

        //Commit
        project.linkingModule.update(updatedLinkingTask)
        User().task = updatedLinkingTask
      }

      // write to repositories defined in properties
      if (!repositories.isEmpty) {
        repositories.foreach(
           repository => {
             val repositoryWriter: RepositoryWriter = new RepositoryWriter(repository._1, repository._2)
             repositoryWriter.post(LinkSpecification.load(prefixes)(new StringReader(linkSpecStr)), SilkConfigExporter.build().toXML.toString)
           }
        )
      }

      //Update link spec variable and notify user
      linkSpecVarCmd & updateStatusCall(errors = Traversable.empty, warnings = warnings.map(_.getMessage), infos = evaluateLinkSpec(User().linkingTask))
    } catch {
      case ex: ValidationException => {
        logger.log(Level.INFO, "Invalid linkage rule")
        updateStatusCall(errors = ex.errors, warnings = Traversable.empty, infos = Traversable.empty)
      }
      case ex: Exception => {
        logger.log(Level.INFO, "Failed to save linkage rule", ex)
        updateStatusCall(errors = ValidationError("Error in back end: " + ex.getMessage) :: Nil, warnings = Traversable.empty, infos = Traversable.empty)
      }
    }
  }

  private def updateStatusCall(errors: Traversable[ValidationError] = currentErrors,
                               warnings: Traversable[String] = currentWarnings,
                               infos: Traversable[String] = currentInfos) = {
    /**Generates a JavaScript expression from a string */
    def toJsExp(str: String) = Str(Text(str).toString)

    /**Generates a JavaScript array from a collection of strings*/
    def toJsArray(messages: Traversable[String]) = JsArray(messages.map(toJsExp).toList)

    /**Generates a JavaScript expression from an error */
    def errorToJsExp(error: ValidationError) = JsObj(("message", Str(error.toString)), ("id", Str(error.id.map(_.toString).getOrElse(""))))

    /**Generates a JavaScript array from a collection of errors*/
    def errorsToJsArray(messages: Traversable[ValidationError]) = JsArray(messages.map(errorToJsExp).toList)

    //Remember current messages
    currentErrors = errors
    currentWarnings = warnings
    currentInfos = infos

    /**Create a call to the update status function */
    Call("updateStatus", errorsToJsArray(errors), toJsArray(warnings), toJsArray(infos)).cmd
  }

  private def evaluateLinkSpec(linkingTask: LinkingTask): Traversable[String] = {
    if(linkingTask.cache.status.isRunning) {
      ("Cache loading") :: Nil
    } else if(linkingTask.cache.status.failed) {
      ("Cache loading failed") :: Nil
    } else if (linkingTask.cache.entities.positive.isEmpty || linkingTask.cache.entities.negative.isEmpty) {
      ("No reference links") :: Nil
    } else {
      val r = LinkageRuleEvaluator(linkingTask.linkSpec.rule, linkingTask.cache.entities)

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
      User().linkingTask.cache.reload(User().project, User().linkingTask)
      JsRaw("").cmd
    }

    JsCmds.Function("reloadCache", Nil, SHtml.ajaxInvoke(reloadCache _)._2.cmd)
  }

  /**
   * Updates the status as soon as the cache has been loaded.
   */
  object CacheListener extends (TaskStatus => Unit) {
    def apply(status: TaskStatus) {
      status match {
        case _ : TaskFinished => {
          partialUpdate(updateStatusCall(infos = evaluateLinkSpec(User().linkingTask)))
        }
        case _ =>
      }
    }
  }
}
