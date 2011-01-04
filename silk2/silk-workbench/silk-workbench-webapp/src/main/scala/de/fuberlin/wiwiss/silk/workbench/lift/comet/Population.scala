//package de.fuberlin.wiwiss.silk.workbench.lift.comet
//
//import net.liftweb.util.Helpers
//import net.liftweb.http.{SHtml, CometActor}
//import xml.{Text, NodeSeq}
//import net.liftweb.common.Box
//import de.fuberlin.wiwiss.silk.config.ConfigWriter
//import de.fuberlin.wiwiss.silk.util.XMLUtils._
//import net.liftweb.http.js.JsCmds.SetHtml
//import de.fuberlin.wiwiss.silk.linkspec.LinkCondition
//import de.fuberlin.wiwiss.silk.workbench.learning.LearningServer
//import de.fuberlin.wiwiss.silk.workbench.learning.PopulationUpdated
//import collection.mutable.{Publisher, Subscriber}
//
//class Population extends CometActor with Subscriber[PopulationUpdated, Publisher[PopulationUpdated]]
//{
//  private var population : Seq[(LinkCondition, Double)] = Seq.empty
//
//  private lazy val infoId = uniqueId + "_info"
//
//  private lazy val infoIn = uniqueId + "_in"
//
//  private lazy val inputArea = Helpers.findKids(defaultXml, "chat", "input")
//
//  private lazy val bodyArea = Helpers.findKids(defaultXml, "chat", "body")
//
//  private lazy val singleLine = Helpers.deepFindKids(bodyArea, "chat", "list")
//
//  LearningServer.subscribe(this)
//
//  override def notify(pub : Publisher[PopulationUpdated], event : PopulationUpdated)
//  {
//    population = LearningServer.population.toSeq.sortWith(_._2 > _._2).take(20)
//    partialUpdate(SetHtml(infoId, displayList))
//  }
//
//  override def render =
//  {
//    bind("chat", bodyArea,
//      AttrBindParam("id", Text(infoId), "id"),
//      "list" -> displayList)
//  }
//
//  private def displayList =
//  {
//    def line(condition : LinkCondition, fitness : Double) =
//    {
//      bind("list", singleLine, "name" -> SHtml.a(showCondition(condition) _, Text("Condition(size=" + condition.rootAggregation.operators.size + " fitness=" + fitness + ")")))
//    }
//
//    val nodes = population.flatMap{case (condition, fitness) => line(condition, fitness)}
//
//    NodeSeq.fromSeq(nodes)
//  }
//
//  private def showCondition(condition : LinkCondition)() =
//  {
//    //Format the condition
//    val formatted = ConfigWriter.serializeLinkCondition(condition).toFormattedString
//
//    SetHtml("condition_id", <pre><tt>{formatted}</tt></pre>)
//  }
//}
