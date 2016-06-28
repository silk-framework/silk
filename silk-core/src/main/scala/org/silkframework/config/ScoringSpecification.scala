package org.silkframework.config

import org.silkframework.entity.EntitySchema
import org.silkframework.rule.ScoringRule
import org.silkframework.util.Identifier

/**
 * This class contains all the required parameters to execute a scoring task.
 */
case class ScoringSpecification(id: Identifier = Identifier.random, selection: DatasetSelection, rules: Seq[ScoringRule], outputs: Seq[Identifier] = Seq.empty) extends TaskSpecification {

  def entityDescription = {
    EntitySchema(
      typeUri = selection.typeUri,
      paths = rules.flatMap(_.toTransform.paths).distinct.toIndexedSeq,
      filter = selection.restriction
    )
  }

}

object ScoringSpecification {

  def fromTransform(task: TransformSpecification) =
    ScoringSpecification(
      id = task.id,
      selection = task.selection,
      rules = task.rules.map(ScoringRule.fromTransform),
      outputs = task.outputs
    )

}
