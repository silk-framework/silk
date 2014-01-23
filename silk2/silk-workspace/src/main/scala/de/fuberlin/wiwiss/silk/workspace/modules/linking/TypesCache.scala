package de.fuberlin.wiwiss.silk.workspace.modules.linking

import de.fuberlin.wiwiss.silk.workspace.Project
import scala.xml.{Node, NodeSeq}
import de.fuberlin.wiwiss.silk.util.DPair

/**
 * Holds the most frequent classes.
 */
class TypesCache() extends Cache[DPair[Set[String]]](DPair.fill(Set[String]())) {

  /** Load the cache value. */
  override protected def update(project: Project, task: LinkingTask) {
    if(value.isEmpty) {
      val sources = task.linkSpec.datasets.map(ds => project.sourceModule.task(ds.sourceId).source)
      val types = for (source <- sources) yield source.retrieveTypes().map(_._1).toSet
      value = types
    }
  }

  /** Writes the current value of this cache to an XML node. */
  override def toXML: NodeSeq = {
    <Types>
      <Source>
        { for(t <- value.source) yield <Type>{t}</Type> }
      </Source>
      <Target>
        { for(t <- value.target) yield <Type>{t}</Type> }
      </Target>
    </Types>
  }

  /** Reads the cache value from an XML node and updates the current value of this cache. */
  override def loadFromXML(node: Node) {
    val sourceTypes = for(typeNode <- node \ "Type" \ "Source") yield typeNode.text
    val targetTypes = for(typeNode <- node \ "Type" \ "Target") yield typeNode.text
    value = DPair(sourceTypes.toSet, targetTypes.toSet)
  }
}
