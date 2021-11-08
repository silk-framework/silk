package org.silkframework.config

import org.silkframework.util.Identifier

trait HasMetaData {

  def id: Identifier

  def metaData: MetaData

  def label(maxLength: Int = MetaData.DEFAULT_LABEL_MAX_LENGTH): String = {
    metaData.formattedLabel(id, maxLength)
  }

  def fullLabel: String = label(Int.MaxValue)

}
