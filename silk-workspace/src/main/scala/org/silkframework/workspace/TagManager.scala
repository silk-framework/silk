package org.silkframework.workspace

import org.silkframework.config.{Tag, TagReference}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.util.Identifier

import java.util.UUID
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
    tags(uri)
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
    * Generates a new unique tag URI.
    */
  def generateTagUri(): String = {
    TagManager.defaultUriPrefix + UUID.randomUUID()
  }

  private def loadIfRequired()(implicit userContext: UserContext): Unit = {
    if(!loaded) {
      provider.readTags(project)
      loaded = true
    }
  }

}

object TagManager {

  final val defaultUriPrefix = "urn:silkframework:tag:"

}
