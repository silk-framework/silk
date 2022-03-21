package org.silkframework.config

import java.time.Instant
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat}
import org.silkframework.util.Uri

import scala.collection.immutable.ListSet
import scala.xml._

/**
  * Holds meta data about a task.
  */
case class MetaData(label: Option[String],
                    description: Option[String] = None,
                    modified: Option[Instant] = None,
                    created: Option[Instant] = None,
                    createdByUser: Option[Uri] = None,
                    lastModifiedByUser: Option[Uri] = None,
                    tags: ListSet[Uri] = ListSet.empty) {

  /**
    * Returns the label if defined or a default string if the label is empty. Truncates the label to maxLength characters.
    *
    * @param defaultLabel A default label that should be returned if the label is empty
    * @param maxLength the max length in characters
    */
  def formattedLabel(defaultLabel: String, maxLength: Int = MetaData.DEFAULT_LABEL_MAX_LENGTH): String = {
    assert(maxLength > 5, "maxLength for task label must be at least 5 chars long")
    val trimmedLabel = label match {
      case Some(l) if l.trim != "" =>
        l.trim
      case _ =>
        defaultLabel
    }
    if(trimmedLabel.length > maxLength) {
      val sideLength = (maxLength - 2) / 2
      trimmedLabel.take(sideLength) + " ... " + trimmedLabel.takeRight(sideLength)
    } else {
      trimmedLabel
    }
  }

  def asNewMetaData(implicit userContext: UserContext): MetaData = {
    val now = Instant.now()
    val user = userUri
    MetaData(
      label,
      description,
      Some(now),
      Some(now),
      user,
      user,
      tags
    )
  }

  def asUpdatedMetaData(implicit userContext: UserContext): MetaData = {
    MetaData(
      label,
      description,
      created = created,
      modified = Some(Instant.now()),
      createdByUser = createdByUser,
      lastModifiedByUser = userUri,
      tags = tags
    )
  }

  private def userUri(implicit userContext: UserContext): Option[Uri] = {
    userContext.user.map(user => Uri(user.uri))
  }
}

object MetaData {

  val DEFAULT_LABEL_MAX_LENGTH = 50

  def empty: MetaData = MetaData(None, None, None, None, None, None)

  /**
    * Generates a nice label from an identifier.
    */
  def labelFromId(id: String): String = {
    if(id.forall(_.isUpper)) {
      id
    } else {
      splitId(id)
    }
  }

  /**
    * Splits an identifier into words separated by spaces.
    */
  private def splitId(id: String): String = {
    val sb = new StringBuilder(id.head.toString)
    for(i <- 1 until id.length) {
      val c = id.charAt(i)
      val prev = id.charAt(i - 1)
      if(c == '_') {
        sb += ' '
      } else if(c.isUpper && !prev.isUpper && prev != '_') {
        sb += ' '
        sb += c
      } else {
        sb += c
      }
    }
    sb.toString
  }

  /**
    * XML serialization format.
    */
  implicit object MetaDataXmlFormat extends XmlFormat[MetaData] {
    /**
      * Deserialize a value from XML.
      */
    def read(node: Node)(implicit readContext: ReadContext): MetaData = {
      MetaData(
        label = Some((node \ "Label").text).filter(_.nonEmpty),
        description = Some((node \ "Description").text).filter(_.nonEmpty),
        modified = (node \ "Modified").headOption.map(node => Instant.parse(node.text)),
        created = (node \ "Created").headOption.map(node => Instant.parse(node.text)),
        createdByUser = (node \ "CreatedByUser").headOption.map(node => node.text),
        lastModifiedByUser = (node \ "LastModifiedByUser").headOption.map(node => node.text),
        tags = ListSet((node \ "Tags" \ "Tag").map(node => Uri(node.text)): _*)
      )
    }

    /**
      * Serialize a value to XML.
      */
    def write(data: MetaData)(implicit writeContext: WriteContext[Node]): Node = {
      val descriptionPCData = PCData(data.description.getOrElse(""))
      <MetaData>
        <Label>{data.label.getOrElse("")}</Label>
        <Description xml:space="preserve">{descriptionPCData}</Description>
        { data.modified.map(instant => <Modified>{instant.toString}</Modified>).toSeq }
        { data.created.map(instant => <Created>{instant.toString}</Created>).toSeq }
        { data.createdByUser.map(userUri => <CreatedByUser>{userUri.uri}</CreatedByUser>).toSeq }
        { data.lastModifiedByUser.map(userUri => <LastModifiedByUser>{userUri.uri}</LastModifiedByUser>).toSeq }
        <Tags>
          { data.tags.map(uri => <Tag>{uri}</Tag>).toSeq }
        </Tags>
      </MetaData>
    }
  }

}
