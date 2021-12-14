package org.silkframework.workspace.tags

import scala.collection.mutable

class TagManager() {

  private val tags = new mutable.HashMap[String, Tag]()

  def getTag(uri: String): Tag = {
    tags(uri)
  }

  def addTag(tag: Tag): TagReference = {
    tags.put(tag.uri, tag)
    TagReference(tag.uri)
  }

}

case class Tag(uri: String, labels: LanguageLabels)

case class LanguageLabels(defaultLabel: String, languageLabels: Map[String, String])

case class TagReference(uri: String)