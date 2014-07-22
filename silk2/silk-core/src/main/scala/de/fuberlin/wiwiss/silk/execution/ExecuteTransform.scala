package de.fuberlin.wiwiss.silk.execution

import de.fuberlin.wiwiss.silk.config.DatasetSelection
import de.fuberlin.wiwiss.silk.runtime.task.Task
import de.fuberlin.wiwiss.silk.dataset.{Dataset}
import de.fuberlin.wiwiss.silk.linkagerule.TransformRule
import de.fuberlin.wiwiss.silk.entity.EntityDescription

/**
 * Executes a set of transformation rules.
 */
class ExecuteTransform(input: Dataset,
                       selection: DatasetSelection,
                       rules: Seq[TransformRule],
                       outputs: Traversable[Dataset] = Traversable.empty) extends Task[Any] {

  def execute(): Unit = {
    // Retrieve entities
    val entityDesc =
      new EntityDescription(
        variable = selection.variable,
        restrictions = selection.restriction,
        paths = rules.flatMap(_.paths).distinct.toIndexedSeq
      )
    val entities = input.source.retrieve(entityDesc)

    // Open outputs
    val sinks = outputs.map(_.sink)
    for(sink <- sinks) sink.open()

    // Transform all entities and write to outputs
    for { entity <- entities
          rule <- rules
          value <- rule(entity)
          sink <- sinks } {
      sink.writeLiteralStatement(entity.uri, rule.targetProperty.uri, value)
    }

    // Close outputs
    for(sink <- sinks) sink.close()
  }
}

object ExecuteTransform {
  def empty = new ExecuteTransform(null, null, null, null)
}