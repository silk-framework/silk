package de.fuberlin.wiwiss.silk.execution

import java.util.logging.{Level, Logger}

import de.fuberlin.wiwiss.silk.config.{TransformSpecification, DatasetSelection}
import de.fuberlin.wiwiss.silk.runtime.task.{TaskContext, Task}
import de.fuberlin.wiwiss.silk.dataset.{DataSource, DataSink}
import de.fuberlin.wiwiss.silk.linkagerule.TransformRule
import de.fuberlin.wiwiss.silk.entity.EntityDescription

/**
 * Executes a set of transformation rules.
 */
class ExecuteTransform(input: DataSource,
                       selection: DatasetSelection,
                       rules: Seq[TransformRule],
                       outputs: Seq[DataSink] = Seq.empty) extends Task {

  def execute(context: TaskContext): Unit = {
    // Retrieve entities
    val entityDesc =
      new EntityDescription(
        variable = selection.variable,
        restrictions = selection.restriction,
        paths = rules.flatMap(_.paths).distinct.toIndexedSeq
      )
    val entities = input.retrieve(entityDesc)

    // Open outputs
    val properties = rules.map(_.targetProperty.uri)
    for(output <- outputs) output.open(properties)

    // Transform all entities and write to outputs
    for { entity <- entities
          rule <- rules } {
      val values = rules.map(_(entity))
      for(output <- outputs)
        output.writeEntity(entity.uri, values)
    }

    // Close outputs
    for(output <- outputs) output.close()
  }
}

object ExecuteTransform {
  def empty = new ExecuteTransform(null, null, null, null)

  /**
   * Create an ExecuteTransform task instance with the provided transform specification.
   *
   * @since 2.6.1
   *
   * @param transform The transform specification.
   * @return An ExecuteTransform instance.
   */
  def apply(transform: TransformSpecification) = new ExecuteTransform(transform.input, transform.selection, transform.rules, transform.outputs)
}