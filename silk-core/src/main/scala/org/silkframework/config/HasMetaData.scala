package org.silkframework.config

import org.silkframework.runtime.activity.UserContext
import org.silkframework.util.Identifier

/**
  * Inherited by classes that provide an identifier and metadata.
  */
trait HasMetaData {

  /**
    * The unique identifier for this object.
    */
  def id: Identifier

  /**
    * The metadata for this object.
    */
  def metaData: MetaData

  /**
    * Returns a label for this object.
    * Per default, it will fall back to generating a label from the identifier, if no label is defined.
    * Subclasses may override this behaviour.
    * Truncates the label to maxLength characters.
    *
    * @param maxLength the max length in characters
    */
  def label(maxLength: Int = Int.MaxValue)(implicit prefixes: Prefixes = Prefixes.empty): String = {
    metaData.formattedLabel(MetaData.labelFromId(id), maxLength)
  }

  /**
    * Returns a label for this object with no length restriction.
    */
  def fullLabel(implicit prefixes: Prefixes = Prefixes.empty): String = label(Int.MaxValue)

  /**
    * Returns a string containing both the label (truncated) and the identifier, e.g., to be used for messages
    * and logging. Returns just the identifier if the label carries no information beyond it.
    */
  def labelAndId(implicit prefixes: Prefixes = Prefixes.empty): String = {
    val truncatedLabel = label(MetaData.DEFAULT_LABEL_MAX_LENGTH)
    if(truncatedLabel == id.toString || truncatedLabel == MetaData.labelFromId(id)) {
      id.toString
    } else {
      s"'$truncatedLabel' ($id)"
    }
  }

  /**
    * Returns the label as set by the user (truncated), or the plain identifier if no label is set.
    * Unlike label(), neither subclass overrides nor identifier beautification apply.
    */
  def labelOrId: String = metaData.formattedLabel(id)

  /**
    * Retrieves the full tags from the meta data.
    */
  def tags()(implicit userContext: UserContext): Set[Tag] = Set.empty

}
