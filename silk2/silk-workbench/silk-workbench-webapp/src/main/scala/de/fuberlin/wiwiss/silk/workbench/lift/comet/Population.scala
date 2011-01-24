//package de.fuberlin.wiwiss.silk.workbench.lift.comet
//
//import net.liftweb.util.Helpers
//import net.liftweb.http.{SHtml, CometActor}
//import xml.{Text, NodeSeq}
//import net.liftweb.common.Box
//import de.fuberlin.wiwiss.silk.util.XMLUtils._
//import net.liftweb.http.js.JsCmds.SetHtml
//import de.fuberlin.wiwiss.silk.workbench.learning.LearningServer
//import de.fuberlin.wiwiss.silk.workbench.learning.PopulationUpdated
//import collection.mutable.{Publisher, Subscriber}
//import de.fuberlin.wiwiss.silk.workbench.learning.tree.LinkConditionNode
//import de.fuberlin.wiwiss.silk.linkspec.{LinkCondition, Aggregation}
//
//class Population extends CometActor with Subscriber[PopulationUpdated, Publisher[PopulationUpdated]]
//{
//  private var population : Seq[(LinkConditionNode, Double)] = Seq.empty
//
//  private var populationSize = 0
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
//    val sortedPopulation = LearningServer.population.toSeq.sortWith(_._2 > _._2)
//    population = sortedPopulation.take(50) ++ sortedPopulation.takeRight(5)
//    populationSize = sortedPopulation.size
//    partialUpdate(SetHtml(infoId, displayList))
//  }
//
//  override def render =
//  {
//    bind("chat", bodyArea,
//      AttrBindParam("id", Text(infoId), "id"),
//      "info" -> populationSize.toString,
//      "list" -> displayList)
//  }
//
//  private def displayList =
//  {
//    def line(conditionNode : LinkConditionNode, fitness : Double) =
//    {
//      val condition = conditionNode.build
//
//      bind("list", singleLine, "name" -> SHtml.a(showCondition(condition) _, Text("Condition(size=" + condition.rootOperator.map{case Aggregation(_, _, ops, _) => ops.size}.getOrElse(0) + " fitness=" + fitness + ")")))
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
//    val formatted = condition.toXML.toFormattedString
//
//    SetHtml("condition_id", <pre><tt>{formatted}</tt></pre>)
//  }
//}
