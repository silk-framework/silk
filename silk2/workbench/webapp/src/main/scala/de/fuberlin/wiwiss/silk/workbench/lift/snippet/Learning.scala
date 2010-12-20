//package de.fuberlin.wiwiss.silk.workbench.lift.snippet
//
//import net.liftweb.http.SHtml
//import de.fuberlin.wiwiss.silk.workbench.lift.util.Widgets
//import de.fuberlin.wiwiss.silk.workbench.project.Project
//import net.liftweb.util.Helpers._
//import xml.NodeSeq
//import de.fuberlin.wiwiss.silk.workbench.learning.LearningServer
//
//class Learning
//{
//  def render(xhtml : NodeSeq) : NodeSeq =
//  {
//    if(Project().cacheLoader.isRunning)
//    {
//      bind("entry", chooseTemplate("choose", "loading", xhtml),
//           "status" -> Widgets.taskProgress(Project().cacheLoader))
//    }
//    else
//    {
//      bind("entry", chooseTemplate("choose", "train", xhtml),
//           "create" -> SHtml.submit("Create", () => LearningServer.createPopulation()),
//           "iterate" -> SHtml.submit("Iterate", () => LearningServer.iteratePopulation()),
//           "progress" -> Widgets.currentTaskProgress(() => LearningServer.currentTask))
//    }
//  }
//}
