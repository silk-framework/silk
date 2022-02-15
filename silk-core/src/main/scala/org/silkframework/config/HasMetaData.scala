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
  def label(maxLength: Int = MetaData.DEFAULT_LABEL_MAX_LENGTH)(implicit prefixes: Prefixes = Prefixes.empty): String = {
    metaData.formattedLabel(MetaData.labelFromId(id), maxLength)
  }

  /**
    * Returns a label for this object with no length restriction.
    */
  def fullLabel(implicit prefixes: Prefixes = Prefixes.empty): String = label(Int.MaxValue)

  /**
    * Returns a string containing both the full label and the identifier, e.g., to be used for logging.
    */
  def labelAndId(implicit prefixes: Prefixes = Prefixes.empty): String = {
    s"'${fullLabel()}' ($id)"
  }

  def tags()(implicit userContext: UserContext): Set[Tag] = Set.empty

}
