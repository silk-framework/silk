package org.silkframework.execution

import org.silkframework.entity.{Entity, EntitySchema, Link, ValueType}
import org.silkframework.entity.paths.{TypedPath, UntypedPath}
import org.silkframework.util.Uri

/**
  * Holds a set of links.
  */
abstract class Links(val linkType: Uri) extends CustomEntities

object Links {

  val linkEntitySchema: EntitySchema = {
    EntitySchema("", IndexedSeq(
      TypedPath(UntypedPath("targetUri"), ValueType.STRING, isAttribute = false),
      TypedPath(UntypedPath("confidence"), ValueType.DOUBLE, isAttribute = false))
    )
  }

  final val sameAsLinkType: Uri = "http://www.w3.org/2002/07/owl#sameAs"

  def createEntity(source: String, target: String, confidence: Double): Entity = {
    Entity(
      uri = source,
      values = IndexedSeq(Seq(target), Seq(confidence.toString)),
      schema = linkEntitySchema
    )
  }

  def createEntityFromLink(link: Link): Entity = {
    Entity(
      uri = link.source,
      values = IndexedSeq(Seq(link.source), Seq(link.target), link.confidence.map(_.toString).toSeq),
      schema = linkEntitySchema
    )
  }

  def createLinkFromEntity(entity: Entity): Link = {
    Link(
      entity.uri,
      entity.evaluate(0).headOption.getOrElse(throw new IllegalArgumentException("Invalid link entity")),
      entity.evaluate(1).headOption.map(_.toDouble)
    )
  }

}
