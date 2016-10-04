package org.silkframework.rule

import org.silkframework.config.TaskSpec
import org.silkframework.entity.{EntitySchema, Path}
import org.silkframework.util.Identifier

/**
 * This class contains all the required parameters to execute a scoring task.
 */
case class ScoringSpec(selection: DatasetSelection, rules: Seq[ScoringRule], outputs: Seq[Identifier] = Seq.empty) extends TaskSpec {

  def entityDescription = {
    EntitySchema(
      typeUri = selection.typeUri,
      paths = rules.flatMap(_.toTransform.paths).distinct.toIndexedSeq,
      filter = selection.restriction
    )
  }

  /**
    * The schemata of the input data for this task.
    * A separate entity schema is returned for each input.
    */
  override def inputSchemataOpt: Option[Seq[EntitySchema]] = Some(Seq(entityDescription))

  /**
    * The schema of the output data.
    * Returns None, if the schema is unknown or if no output is written by this task.
    */
  override def outputSchemaOpt: Option[EntitySchema] = {
    Some(
      EntitySchema(
        typeUri = selection.typeUri,
        paths = rules.map(_.target).map(Path(_)).toIndexedSeq
      )
    )
  }

  override def referencedTasks =  Set(selection.inputId)
}

object ScoringSpec {

  def fromTransform(task: TransformSpec) =
    ScoringSpec(
      selection = task.selection,
      rules = task.rules.map(ScoringRule.fromTransform),
      outputs = task.outputs
    )

}
