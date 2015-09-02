package de.fuberlin.wiwiss.silk.rule.evaluation

import de.fuberlin.wiwiss.silk.rule.TransformRule

case class DetailedEntity(uri: String, values: Seq[Value], rules: Seq[TransformRule]) {
}
