package bootstrap.liftweb

import _root_.net.liftweb.http._
import _root_.net.liftweb.sitemap._
import _root_.net.liftweb.sitemap.Loc._
import de.fuberlin.wiwiss.silk.plugins.Plugins
import js.jquery.JQuery14Artifacts
import net.liftweb.widgets.autocomplete.AutoComplete
import scala.xml.Text
import de.fuberlin.wiwiss.silk.workbench.workspace.{FileUser, User}
import de.fuberlin.wiwiss.silk.plugins.jena.JenaPlugins

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
        Menu(Loc("Sample Links", List("sampleLinks"), "Sample Links", ifLinkingTaskOpen)) ::
        Menu(Loc("Reference Links", List("referenceLinks"), "Reference Links", ifLinkingTaskOpen)) ::
        Menu(Loc("Learn", List("learn"), "Learn", ifLinkingTaskOpen)) ::
        Nil

    LiftRules.setSiteMap(SiteMap(entries:_*))

    LiftRules.dispatch.prepend(Api.dispatch)
  }
}
