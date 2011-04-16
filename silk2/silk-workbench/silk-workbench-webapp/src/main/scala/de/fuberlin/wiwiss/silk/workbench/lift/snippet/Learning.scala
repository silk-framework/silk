//package de.fuberlin.wiwiss.silk.workbench.lift.snippet
//
//import net.liftweb.http.SHtml
//import de.fuberlin.wiwiss.silk.workbench.lift.util.Widgets
//import net.liftweb.util.Helpers._
//import xml.NodeSeq
//import de.fuberlin.wiwiss.silk.workbench.learning.LearningServer
//import de.fuberlin.wiwiss.silk.workbench.workspace.User
//
//class Learning
//{
//  def toolbar(xhtml : NodeSeq) : NodeSeq =
//  {
//    if(User().linkingTask.cacheLoader.isRunning)
//    {
//      bind("entry", chooseTemplate("choose", "loading", xhtml),
//           "status" -> Widgets.taskProgress(User().linkingTask.cacheLoader))
//    }
//    else
//    {
//      var iterations = 1
//
//      bind("entry", chooseTemplate("choose", "train", xhtml),
//           "create" -> SHtml.submit("Create", () => LearningServer.generatePopulation()),
//           "iterations" -> SHtml.number(iterations, iterations = _, 1, 100),
//           "iterate" -> SHtml.submit("Iterate", () => LearningServer.iteratePopulation(iterations)),
//           "clean" -> SHtml.submit("Clean", () => LearningServer.cleanPopulation()),
//           "progress" -> Widgets.currentTaskProgress(() => LearningServer.currentTask))
//    }
//  }
//
//  def content(xhtml : NodeSeq) : NodeSeq =
//  {
//    if(User().linkingTask.cacheLoader.isRunning)
//    {
//      chooseTemplate("choose", "loading", xhtml)
//    }
//    else
//    {
//      chooseTemplate("choose", "train", xhtml)
//    }
//  }
//}
