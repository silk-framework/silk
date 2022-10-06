package org.silkframework.serialization.json

import io.swagger.v3.oas.annotations.media.{ArraySchema, Schema}
import org.silkframework.config.{MetaData, Tag}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.users.User
import org.silkframework.serialization.json.TransformedJsonFormat.TransformableJsonFormat
import org.silkframework.util.Uri
import org.silkframework.workspace.TagManager
import play.api.libs.json.{Format, Json}

import java.time.Instant

/**
  * JSON serializers for meta data objects.
  * Adds classes that mirror the internal meta data classes for the purpose of simple serialization and OpenAPI documentation.
  */
object MetaDataSerializers {

  @Schema(description = "A full tag definition that includes the tag URI and its label")
  case class FullTag(uri: String, label: String)

  object FullTag {
    def fromTag(tag: Tag): FullTag = {
      FullTag(tag.uri, tag.label)
    }
  }

  @Schema(description = "Full user info that contains both the URI and the label.")
  case class UserInfo(uri: String, label: String)

  object UserInfo {
    def fromUri(userUri: Uri): UserInfo = {
      UserInfo(userUri, User.labelFromUri(userUri))
    }

    def fromUser(user: User): UserInfo = {
      UserInfo(user.uri, user.label)
    }
  }

  @Schema(description = "Plain meta data object")
  case class MetaDataPlain(label: Option[String],
                           description: Option[String] = None,
                           modified: Option[Instant] = None,
                           created: Option[Instant] = None,
                           createdByUser: Option[String] = None,
                           lastModifiedByUser: Option[String] = None,
                           @ArraySchema(schema = new Schema(implementation = classOf[String], required = false, nullable = true))
                           tags: Option[Set[String]] = None) {
    def toMetaData: MetaData = {
      MetaDataPlain.toMetaData(this)
    }
  }

  object MetaDataPlain {

    def fromMetaData(md: MetaData): MetaDataPlain = {
      MetaDataPlain(
        label = md.label,
        description = md.description,
        modified = md.modified,
        created = md.created,
        createdByUser = md.createdByUser.map(_.uri),
        lastModifiedByUser = md.lastModifiedByUser.map(_.uri),
        tags = if(md.tags.isEmpty) None else Some(md.tags.map(_.uri))
      )
    }

    def toMetaData(md: MetaDataPlain): MetaData = {
      MetaData(
        label = md.label,
        description = md.description,
        modified = md.modified,
        created = md.created,
        createdByUser = md.createdByUser.map(new Uri(_)),
        lastModifiedByUser = md.lastModifiedByUser.map(new Uri(_)),
        tags = md.tags.getOrElse(Set.empty).map(new Uri(_))
      )
    }
  }

  @Schema(description = "Expanded meta data object. Contains full tags instead of URI references.")
  case class MetaDataExpanded(label: Option[String],
                              description: Option[String] = None,
                              modified: Option[Instant] = None,
                              created: Option[Instant] = None,
                              createdByUser: Option[UserInfo] = None,
                              lastModifiedByUser: Option[UserInfo] = None,
                              @ArraySchema(schema = new Schema(implementation = classOf[FullTag]))
                              tags: Set[FullTag] = Set.empty)

  object MetaDataExpanded {
    def fromMetaData(md: MetaData, tags: TagManager)
                    (implicit userContext: UserContext): MetaDataExpanded = {
      val expandedTags = md.tags.map(uri => tags.getTag(uri)).map(FullTag.fromTag)
      MetaDataExpanded(md.label, md.description, md.modified, md.created, md.createdByUser.map(UserInfo.fromUri), md.lastModifiedByUser.map(UserInfo.fromUri), expandedTags)
    }
  }

  implicit val tagFormat: Format[FullTag] = Json.format[FullTag]
  implicit val userFormat: Format[UserInfo] = Json.format[UserInfo]
  implicit val metaDataFormat: Format[MetaDataPlain] = Json.format[MetaDataPlain]
  implicit val metaDataExpandedFormat: Format[MetaDataExpanded] = Json.format[MetaDataExpanded]

  implicit val metaDataJsonFormat: JsonFormat[MetaData] = new PlayJsonFormat[MetaDataPlain]().map(MetaDataPlain.toMetaData, MetaDataPlain.fromMetaData)

}
