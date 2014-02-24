package de.fuberlin.wiwiss.silk.linkagerule.evaluation

import de.fuberlin.wiwiss.silk.linkagerule.TransformRule

case class DetailedEntity(uri: String, values: Seq[Value], rules: Seq[TransformRule]) {
}
