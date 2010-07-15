package de.fuberlin.wiwiss.silk.server.view.comet

import xml.{Text, NodeSeq}
import net.liftweb.common.{Full, Empty, Box}
import net.liftweb.util.Helpers
import net.liftweb.http.{SHtml, CometActor}
import de.fuberlin.wiwiss.silk.server.model.{Dataset, Server}

class DatasetManager extends CometActor
{
    override def defaultPrefix = Full("comet")

    private lazy val inputNode = Helpers.findKids(defaultXml, "comet", "input")

    private lazy val bodyNode = Helpers.findKids(defaultXml, "comet", "body")

    private lazy val rowNode = Helpers.deepFindKids(bodyNode, "comet", "row")

    override lazy val fixedRender : Box[NodeSeq] =
    {
        inputNode
    }

    override def render =
    {
        bind("comet", bodyNode, "row" -> generateRows)
    }

    private def generateRows =
    {
        val datasets = Server.datasets.toSeq.sortBy(_.name)

        val nodes = datasets.flatMap(generateRow)

        NodeSeq.fromSeq(nodes)
    }

    private def generateRow(dataset : Dataset) =
    {
        bind("row", rowNode,
             "name" -> Text(dataset.name),
             "sourceInstances" -> Text(dataset.sourceInstanceCount.toString),
             "targetInstances" -> Text(dataset.targetInstanceCount.toString))
    }
}
