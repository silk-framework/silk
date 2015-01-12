package de.fuberlin.wiwiss.silk.workspace.modules.dataset

import de.fuberlin.wiwiss.silk.workspace.Project
import de.fuberlin.wiwiss.silk.workspace.modules.Cache

import scala.xml.Node

/**
 * Holds the most frequent classes.
 */
class TypesCache() extends Cache[DatasetTask, Seq[(String, Double)]](Seq[(String, Double)]()) {

  /** Load the cache value. */
  override protected def update(project: Project, task: DatasetTask) =  {
    if(value.isEmpty) {
      val dataSource = task.source
      val types = dataSource.retrieveTypes().toSeq
      value = types
      true
    } else {
      false
    }
  }

  /** Writes the current value of this cache to an XML node. */
  override def serialize: Node = {
    <Types>
      { for((uri, frequency) <- value) yield <Type frequency={frequency.toString}>{uri}</Type> }
    </Types>
  }

  /** Reads the cache value from an XML node and updates the current value of this cache. */
  override def deserialize(node: Node) {
    for(typeNode <- node \ "Type";
        frequencyNode <- typeNode \ "@frequency")
        yield (typeNode.text, frequencyNode.text.toDouble)
  }
}
