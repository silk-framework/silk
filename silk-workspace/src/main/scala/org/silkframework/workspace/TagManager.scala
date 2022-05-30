package org.silkframework.workspace

import org.silkframework.config.{Tag, TagReference}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.util.{Identifier, Uri}

import java.net.{URLDecoder, URLEncoder}
import java.util.logging.Logger
import scala.collection.mutable

class TagManager(project: Identifier, provider: WorkspaceProvider) {
  private val log: Logger = Logger.getLogger(this.getClass.getName)

  private val tags = new mutable.HashMap[String, Tag]()

  private var loaded: Boolean = false

  def allTags()(implicit userContext: UserContext): Iterable[Tag] = synchronized {
    loadIfRequired()
    tags.values.toArray[Tag]
  }

  def getTag(uri: String)(implicit userContext: UserContext): Tag = synchronized {
    loadIfRequired()
    tags.get(uri) match {
      case Some(tag) => tag
      case None =>
        // When this happens it would be a bug. Return a usable tag, but make it obvious that this is not expected.
        log.warning(s"Tag $uri is referenced, but has not been found in project $project.")
        val tagLabel = decodeTagLabel(uri)
        Tag(uri, s"$tagLabel (generated label)")
    }
  }

  def putTag(tag: Tag)(implicit userContext: UserContext): TagReference = synchronized {
    loadIfRequired()
    provider.putTag(project, tag)
    tags.put(tag.uri, tag)
    TagReference(tag.uri)
  }

  def deleteTag(tagUri: String)(implicit userContext: UserContext): Unit = synchronized {
    loadIfRequired()
    provider.deleteTag(project, tagUri)
    tags.remove(tagUri)
  }

  /**
    * Generates a tag URI.
    * Tags with the same label will receive the same URI.
    */
  def generateTagUri(label: String): String = {
    TagManager.defaultUriPrefix + URLEncoder.encode(label, "UTF8")
  }

  private def decodeTagLabel(uri: String): String = {
    if(uri.startsWith(TagManager.defaultUriPrefix)) {
      URLDecoder.decode(uri.stripPrefix(TagManager.defaultUriPrefix), "UTF8")
    } else {
      Uri.urlDecodedLocalNameOfURI(uri)
    }
  }

  private def loadIfRequired()(implicit userContext: UserContext): Unit = {
    if(!loaded) {
      for(tag <- provider.readTags(project)) {
        tags += ((tag.uri, tag))
      }
      loaded = true
    }
  }

}

object TagManager {

  final val defaultUriPrefix = "urn:silkframework:tag:"

}
