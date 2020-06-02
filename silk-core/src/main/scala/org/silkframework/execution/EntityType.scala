package org.silkframework.execution

import org.silkframework.entity.paths.{TypedPath, UntypedPath}
import org.silkframework.entity.{Entity, EntitySchema, Link, ValueType}
import org.silkframework.util.Uri

// To be included into EntityHolder
abstract class EntityType[T] {

  def fromEntity(entity: Entity): T

  def toEntity(value: T): Entity

}

case class LinksEntityType(linkType: Uri) extends EntityType[Link] {

  val linkEntitySchema: EntitySchema = {
    EntitySchema("", IndexedSeq(
      TypedPath(UntypedPath("targetUri"), ValueType.STRING, isAttribute = false),
      TypedPath(UntypedPath("confidence"), ValueType.DOUBLE, isAttribute = false))
    )
  }

  final val sameAsLinkType: Uri = "http://www.w3.org/2002/07/owl#sameAs"

  override def toEntity(link: Link): Entity = {
    Entity(
      uri = link.source,
      values = IndexedSeq(Seq(link.source), Seq(link.target), link.confidence.map(_.toString).toSeq),
      schema = linkEntitySchema
    )
  }

  override def fromEntity(entity: Entity): Link = {
    Link(
      entity.uri,
      entity.evaluate(0).headOption.getOrElse(throw new IllegalArgumentException("Invalid link entity")),
      entity.evaluate(1).headOption.map(_.toDouble)
    )
  }


}

object LinksEntityType {

}


