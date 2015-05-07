package de.fuberlin.wiwiss.silk.workspace.modules.dataset

import de.fuberlin.wiwiss.silk.dataset.Dataset
import de.fuberlin.wiwiss.silk.runtime.activity.{Activity, ActivityContext}

/**
 * Holds the most frequent types.
 */
class TypesCache(dataset: Dataset) extends Activity[Types] {

  override def run(context: ActivityContext[Types]): Unit = {
    val dataSource = dataset.source
    val types = Types(dataSource.retrieveTypes().toSeq)
    context.value() = types
  }
}