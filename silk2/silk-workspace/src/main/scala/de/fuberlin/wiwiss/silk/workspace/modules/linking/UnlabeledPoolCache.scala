package de.fuberlin.wiwiss.silk.workspace.modules.linking

import de.fuberlin.wiwiss.silk.entity.{EntityDescription, Link}
import de.fuberlin.wiwiss.silk.workspace.Project
import scala.xml.{Node, NodeSeq}
import de.fuberlin.wiwiss.silk.util.DPair
import de.fuberlin.wiwiss.silk.workspace.modules.Cache

class UnlabeledPoolCache extends Cache[LinkingTask, Seq[Link]](Seq.empty) {

  /**
   * Loads the unlabeled pool.
   */
  override def update(project: Project, task: LinkingTask) = {
    false
  }

  override def serialize: NodeSeq = {
    <Pool> { for(link <- value) yield link.toXML } </Pool>
  }

  override def deserialize(node: Node) {
//    value =
//      for(linkNode <- node \ "Pool" \ "Link") yield {
//        Link.fromXML(linkNode)
//      }
    ???
  }
}
