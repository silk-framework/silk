package org.silkframework.config

import org.silkframework.entity.EntitySchema
import org.silkframework.rule.ScoringRule
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

}

object ScoringSpec {

  def fromTransform(task: TransformSpec) =
    ScoringSpec(
      selection = task.selection,
      rules = task.rules.map(ScoringRule.fromTransform),
      outputs = task.outputs
    )

}
