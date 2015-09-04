package de.fuberlin.wiwiss.silk.config

import de.fuberlin.wiwiss.silk.entity.EntityDescription
import de.fuberlin.wiwiss.silk.rule.{ScoringRule, TransformRule}
import de.fuberlin.wiwiss.silk.util.Identifier

/**
 * This class contains all the required parameters to execute a scoring task.
 */
case class ScoringSpecification(id: Identifier = Identifier.random, selection: DatasetSelection, rules: Seq[ScoringRule], outputs: Seq[Identifier] = Seq.empty) {

  def entityDescription = {
    new EntityDescription(
      variable = selection.variable,
      restrictions = selection.restriction,
      paths = rules.flatMap(_.toTransform.paths).distinct.toIndexedSeq
    )
  }

}
