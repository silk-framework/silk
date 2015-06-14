package de.fuberlin.wiwiss.silk.execution

import de.fuberlin.wiwiss.silk.config.{DatasetSelection, TransformSpecification}
import de.fuberlin.wiwiss.silk.dataset.{DataSink, DataSource}
import de.fuberlin.wiwiss.silk.entity.EntityDescription
import de.fuberlin.wiwiss.silk.linkagerule.TransformRule
import de.fuberlin.wiwiss.silk.runtime.activity.{Activity, ActivityContext}

/**
 * Executes a set of transformation rules.
 */
class ExecuteTransform(input: DataSource,
                       selection: DatasetSelection,
                       rules: Seq[TransformRule],
                       outputs: Seq[DataSink] = Seq.empty) extends Activity[Unit] {

  require(rules.count(_.targetProperty.isEmpty) <= 1, "Only one rule with empty target property (subject rule) allowed.")

  private val subjectRule = rules.find(_.targetProperty.isEmpty)

  private val propertyRules = rules.filter(_.targetProperty.isDefined)

  def run(context: ActivityContext[Unit]): Unit = {
    // Retrieve entities
    val entityDesc =
      new EntityDescription(
        variable = selection.variable,
        restrictions = selection.restriction,
        paths = rules.flatMap(_.paths).distinct.toIndexedSeq
      )
    val entities = input.retrieve(entityDesc)

    try {
      // Open outputs
      val properties = propertyRules.map(_.targetProperty.get.uri)
      for(output <- outputs) output.open(properties)

      // Transform all entities and write to outputs
      var count = 0
      for(entity <- entities) {
        val uri = subjectRule.flatMap(_(entity).headOption).getOrElse(entity.uri)
        val values = propertyRules.map(_(entity))
        for (output <- outputs)
          output.writeEntity(uri, values)
        count += 1
      }
      context.status.update(s"$count entities written to ${outputs.size} outputs", 1.0)
    } finally {
      // Close outputs
      for (output <- outputs) output.close()
    }
  }
}

object ExecuteTransform {
  /**
   * Create an ExecuteTransform task instance with the provided transform specification.
   *
   * @since 2.6.1
   *
   * @param transform The transform specification.
   * @return An ExecuteTransform instance.
   */
  def apply(input: DataSource, transform: TransformSpecification) = new ExecuteTransform(input, transform.selection, transform.rules, transform.outputs.map(_.sink))
}