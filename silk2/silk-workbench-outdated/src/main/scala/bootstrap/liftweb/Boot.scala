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

package bootstrap.liftweb

import _root_.net.liftweb.sitemap._
import _root_.net.liftweb.sitemap.Loc._
import de.fuberlin.wiwiss.silk.plugins.Plugins
import scala.xml.Text
import de.fuberlin.wiwiss.silk.workspace.{FileUser, User}
import de.fuberlin.wiwiss.silk.plugins.jena.JenaPlugins
import net.liftweb.http._
import js.jquery.JQuery14Artifacts
import net.liftweb.util.NamedPF
import net.liftmodules.widgets.autocomplete.AutoComplete

/**
  * Configures the Silk Workbench WebApp.
  */
class Boot {

  object UserManager extends SessionVar[User](new FileUser) {
    override protected def onShutdown(session : CleanUpParam) {
      is.dispose()
    }
  }

  def boot {
    User.userManager = UserManager.is _

    Plugins.register()
    JenaPlugins.register()

    LiftRules.jsArtifacts = JQuery14Artifacts
    LiftRules.maxMimeSize = 1024L * 1024L * 1024L
    LiftRules.maxMimeFileSize = 1024L * 1024L * 1024L

    //Workaround to fix hardcoded resource paths
    if(!LiftRules.context.path.isEmpty) {
      LiftRules.resourceServerPath = LiftRules.context.path.stripPrefix("/") + "/classpath"

      ResourceServer.allow({
        case _ => true
      })

      LiftRules.statelessDispatchTable.prepend(NamedPF("Classpath service") {
        case r@Req(mainPath :: subPath, suffx, _) if (mainPath == "classpath") =>
          ResourceServer.findResourceInClasspath(r, r.path.wholePath.drop(1))
      })
    }

    AutoComplete.init()

    // where to search snippet
    LiftRules.addToPackages("de.fuberlin.wiwiss.silk.workbench.lift")

    // Build SiteMap
    val ifLinkingTaskOpen = If(() => User().linkingTaskOpen, () => RedirectResponse("index"))

    val workspaceText = LinkText[Unit](_ => Text(if(User().projectOpen) "Workspace: " + User().project.name else "Workspace"))
    val linkSpecText = LinkText[Unit](_ => Text("Editor: " + User().linkingTask.name))

    val entries =
        Menu(Loc("Workspace", List("index"), workspaceText)) ::
        Menu(Loc("Editor", List("editor"), linkSpecText, ifLinkingTaskOpen)) ::
        Menu(Loc("Generate Links", List("generateLinks"), "Generate Links", ifLinkingTaskOpen)) ::
        Menu(Loc("Learn", List("learn"), "Learn", ifLinkingTaskOpen)) ::
        Menu(Loc("Reference Links", List("referenceLinks"), "Reference Links", ifLinkingTaskOpen)) ::
        Menu(Loc("Population", List("population"), "Population", ifLinkingTaskOpen)) ::
        Nil

    LiftRules.setSiteMap(SiteMap(entries:_*))

    LiftRules.dispatch.prepend(Api.dispatch)
  }
}
