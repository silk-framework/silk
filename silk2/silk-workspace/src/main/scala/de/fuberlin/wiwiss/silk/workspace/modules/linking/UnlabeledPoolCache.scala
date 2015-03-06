package de.fuberlin.wiwiss.silk.workspace.modules.linking

import de.fuberlin.wiwiss.silk.config.LinkSpecification
import de.fuberlin.wiwiss.silk.entity.Link
import de.fuberlin.wiwiss.silk.workspace.Project
import de.fuberlin.wiwiss.silk.workspace.modules.Cache

import scala.xml.Node

// TODO not yet implemented
class UnlabeledPoolCache extends Cache[LinkSpecification, Seq[Link]](Seq.empty) {

  /**
   * Loads the unlabeled pool.
   */
  override def update(project: Project, task: LinkSpecification) = {
    false
  }

  override def serialize: Node = {
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
