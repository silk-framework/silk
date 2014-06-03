package de.fuberlin.wiwiss.silk.workspace.modules.source

import de.fuberlin.wiwiss.silk.workspace.Project
import scala.xml.{Node, NodeSeq}
import de.fuberlin.wiwiss.silk.workspace.modules.Cache

/**
 * Holds the most frequent classes.
 */
class TypesCache() extends Cache[SourceTask, Seq[(String, Double)]](Seq[(String, Double)]()) {

  /** Load the cache value. */
  override protected def update(project: Project, task: SourceTask) =  {
    if(value.isEmpty) {
      val dataSource = task.source.dataSource
      val types = dataSource.retrieveTypes().toSeq
      value = types
      true
    } else {
      false
    }
  }

  /** Writes the current value of this cache to an XML node. */
  override def toXML: NodeSeq = {
    <Types>
      { for((uri, frequency) <- value) yield <Type frequency={frequency.toString}>{uri}</Type> }
    </Types>
  }

  /** Reads the cache value from an XML node and updates the current value of this cache. */
  override def loadFromXML(node: Node) {
    for(typeNode <- node \ "Types" \ "Type";
        frequencyNode <- typeNode \ "@frequency")
        yield (typeNode.text, frequencyNode.text.toDouble)
  }
}
