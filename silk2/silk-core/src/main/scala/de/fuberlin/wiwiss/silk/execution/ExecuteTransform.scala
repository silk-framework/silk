package de.fuberlin.wiwiss.silk.execution

import java.util.logging.{Level, Logger}

import de.fuberlin.wiwiss.silk.config.{TransformSpecification, DatasetSelection}
import de.fuberlin.wiwiss.silk.runtime.task.Task
import de.fuberlin.wiwiss.silk.dataset.{DataSource, DataSink}
import de.fuberlin.wiwiss.silk.linkagerule.TransformRule
import de.fuberlin.wiwiss.silk.entity.EntityDescription

/**
 * Executes a set of transformation rules.
 */
class ExecuteTransform(input: DataSource,
                       selection: DatasetSelection,
                       rules: Seq[TransformRule],
                       outputs: Seq[DataSink] = Seq.empty) extends Task[Any] {

  def execute(): Unit = {

    logger.log(Level.INFO, "Executing transform.")

    // Retrieve entities
    val entityDesc =
      new EntityDescription(
        variable = selection.variable,
        restrictions = selection.restriction,
        paths = rules.flatMap(_.paths).distinct.toIndexedSeq
      )
    val entities = input.retrieve(entityDesc)

    // Open outputs
    for(output <- outputs) output.open()

    // Transform all entities and write to outputs
    for { entity <- entities
          rule <- rules
          value <- rule(entity)
          output <- outputs } {
      output.writeLiteralStatement(entity.uri, rule.targetProperty.uri, value)
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