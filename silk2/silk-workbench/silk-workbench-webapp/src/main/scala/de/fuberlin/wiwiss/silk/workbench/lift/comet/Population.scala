//package de.fuberlin.wiwiss.silk.workbench.lift.comet
//
//import net.liftweb.util.Helpers
//import net.liftweb.http.{SHtml, CometActor}
//import xml.{Text, NodeSeq}
//import net.liftweb.common.Box
//import de.fuberlin.wiwiss.silk.util.XMLUtils._
//import net.liftweb.http.js.JsCmds.SetHtml
//import collection.mutable.{Publisher, Subscriber}
//import de.fuberlin.wiwiss.silk.workbench.learning.tree.LinkConditionNode
//import de.fuberlin.wiwiss.silk.linkspec.{LinkCondition, Aggregation}
//import de.fuberlin.wiwiss.silk.workbench.learning.{Individual, LearningServer, PopulationUpdated}
//
//class Population extends CometActor with Subscriber[PopulationUpdated, Publisher[PopulationUpdated]]
//{
//  private var individuals : Seq[Individual] = Seq.empty
//
//  private var individualCount = 0
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
//    val sortedIndividuals = LearningServer.population.individuals.toSeq.sortBy(-_.fitness)
//    individuals = sortedIndividuals.take(50) ++ sortedIndividuals.takeRight(5)
//    individualCount = sortedIndividuals.size
//    partialUpdate(SetHtml(infoId, displayList))
//  }
//
//  override def render =
//  {
//    bind("chat", bodyArea,
//      AttrBindParam("id", Text(infoId), "id"),
//      "info" -> individualCount.toString,
//      "list" -> displayList)
//  }
//
//  private def displayList =
//  {
//    def line(ind : Individual) =
//    {
//      val condition = ind.node.build
//
//      bind("list", singleLine, "name" -> SHtml.a(showCondition(condition) _, Text("Condition(size=" + condition.rootOperator.map{case Aggregation(_, _, ops, _) => ops.size}.getOrElse(0) + " fitness=" + ind.fitness + ")")))
//    }
//
//    val nodes = individuals.flatMap(line)
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
