package org.silkframework.rule.evaluation

import org.silkframework.rule.TransformRule

case class DetailedEntity(uris: Seq[String], values: Seq[Value], rules: Seq[TransformRule])