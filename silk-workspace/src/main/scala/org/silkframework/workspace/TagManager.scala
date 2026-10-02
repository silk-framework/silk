package org.silkframework.workspace

import org.silkframework.config.{Tag, TagReference, TaskSpec}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.validation.RequestException
import org.silkframework.util.{Identifier, Uri}
import org.silkframework.workspace.changes.{ChangeJournal, RemoveTag, SetTag}

import java.net.{URLDecoder, URLEncoder}
import java.util.logging.Logger
import scala.collection.mutable
import scala.util.Try

/**
  * Manages the tags of a project.
  *
  * @param changeJournal The journal that records each tag change.
  */
class TagManager(project: Identifier, provider: WorkspaceProvider, changeJournal: ChangeJournal) {
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

  /** The given tag URIs that are not tags of the project. */
  def missingTags(uris: Set[Uri])(implicit userContext: UserContext): Set[Uri] = synchronized {
    loadIfRequired()
    uris.filterNot(uri => tags.contains(uri.uri))
  }

  /** Adds a tag or replaces the tag of that URI. Recorded in the change journal, unless the tag is there as given. */
  def putTag(tag: Tag)(implicit userContext: UserContext): TagReference = synchronized {
    loadIfRequired()
    provider.putTag(project, tag)
    val before = tags.put(tag.uri, tag)
    if(!before.contains(tag)) {
      changeJournal.record(SetTag(before, tag))
    }
    TagReference(tag.uri)
  }

  /** Removes a tag, also if tasks still have it; [[Project.removeTag]] refuses that. Recorded in the change journal, if there is such a tag. */
  def deleteTag(tagUri: String)(implicit userContext: UserContext): Unit = synchronized {
    loadIfRequired()
    provider.deleteTag(project, tagUri)
    tags.remove(tagUri).foreach(tag => changeJournal.record(RemoveTag(tag)))
  }

  /**
    * Creates a tag for a label, which is normalized.
    * The URI is generated from the label, unless one is given.
    */
  def createTag(label: String, uri: Option[String] = None)(implicit userContext: UserContext): Tag = {
    val normalizedLabel = TagManager.normalizeLabel(label)
    val tag = Tag(Uri(uri.getOrElse(TagManager.generateTagUri(normalizedLabel))), normalizedLabel)
    putTag(tag)
    tag
  }

  private def decodeTagLabel(uri: String): String = {
    TagManager.labelOfGeneratedUri(uri).getOrElse(Uri.urlDecodedLocalNameOfURI(uri))
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

/** A tag is not removed, as tasks or the project itself still have it; answers 409. Names them, the tasks with their ids. */
case class TagInUseException(project: Identifier, tag: Tag, users: Seq[String])
  extends RequestException(s"Tag '${tag.label}' in project '$project' is still used by ${users.mkString(", ")}.", None) {

  override def errorTitle: String = "Conflict"

  override def httpErrorCode: Option[Int] = Some(409)
}

object TagInUseException {

  /** Throws if one of `tasks`, the tasks that have the tag, or the project itself has the tag. */
  def check(project: Project, tag: Tag, tasks: Seq[ProjectTask[_ <: TaskSpec]])(implicit userContext: UserContext): Unit = {
    // With the id, as the label alone does not tell which task to change.
    val users = tasks.map(task => s"task ${task.labelAndId}") ++
      Seq("the project itself").filter(_ => project.config.metaData.tags.contains(tag.uri))
    if(users.nonEmpty) {
      throw TagInUseException(project.id, tag, users)
    }
  }
}

object TagManager {

  final val defaultUriPrefix = "urn:silkframework:tag:"

  /** Trims a tag label and collapses its whitespace. */
  def normalizeLabel(label: String): String = label.trim.replaceAll("\\s+", " ")

  /**
    * Generates a tag URI.
    * Tags with the same label will receive the same URI.
    */
  def generateTagUri(label: String): String = {
    defaultUriPrefix + URLEncoder.encode(label, "UTF8")
  }

  /** The label that a generated tag URI has been generated from. None for any other URI. */
  def labelOfGeneratedUri(uri: String): Option[String] = {
    if(uri.startsWith(defaultUriPrefix)) {
      Try(URLDecoder.decode(uri.stripPrefix(defaultUriPrefix), "UTF8")).toOption
    } else {
      None
    }
  }

}
