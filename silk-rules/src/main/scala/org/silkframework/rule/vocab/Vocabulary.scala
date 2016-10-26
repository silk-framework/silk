package org.silkframework.rule.vocab

case class Vocabulary(info: Info, classes: Traversable[VocabularyClass], properties: Traversable[VocabularyProperty])

case class VocabularyClass(info: Info, subClassOf: Option[VocabularyClass])

case class VocabularyProperty(info: Info, domain: Option[VocabularyClass], range: Option[VocabularyClass])

case class Info(uri: String, label: String, description: String)
