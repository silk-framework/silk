package org.silkframework.workspace

import org.silkframework.config.{Tag, TagReference}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.util.Identifier

import java.net.URLEncoder
import scala.collection.mutable

class TagManager(project: Identifier, provider: WorkspaceProvider) {

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
      case None => throw new NoSuchElementException(s"Tag $uri is referenced, but has not been found in project $project.")
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
