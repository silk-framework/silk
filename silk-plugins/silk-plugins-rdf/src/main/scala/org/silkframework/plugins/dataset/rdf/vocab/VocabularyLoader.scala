package org.silkframework.plugins.dataset.rdf.vocab

import org.silkframework.dataset.rdf.SparqlEndpoint
import org.silkframework.rule.vocab.{Info, Vocabulary, VocabularyClass, VocabularyProperty}

private class VocabularyLoader(endpoint: SparqlEndpoint) {

  def retrieveVocabulary(uri: String): Vocabulary = {
    val classes = retrieveClasses(uri)
    Vocabulary(
      info = Info(uri, None, None),
      classes = classes,
      properties = retrieveProperties(uri, classes)
    )
  }

  def retrieveClasses(uri: String): Traversable[VocabularyClass] = {
    val classQuery =
      s"""
         | PREFIX owl: <http://www.w3.org/2002/07/owl#>
         | PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
         |
         | SELECT * WHERE {
         |   GRAPH <$uri> {
         |     { ?c a owl:Class }
         |     UNION
         |     { ?c a rdfs:Class }
         |     OPTIONAL { ?c rdfs:label ?label }
         |     OPTIONAL { ?c rdfs:comment ?desc }
         |   }
         | }
      """.stripMargin

    for(result <- endpoint.select(classQuery).bindings) yield {
      VocabularyClass(
        info =
          Info(
            uri = result("c").value,
            label = result.get("label").map(_.value),
            description = result.get("desc").map(_.value)
          )
      )
    }
  }

  def retrieveProperties(uri: String, classes: Traversable[VocabularyClass]): Traversable[VocabularyProperty] = {
    val propertyQuery =
      s"""
         | PREFIX owl: <http://www.w3.org/2002/07/owl#>
         | PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
         |
         | SELECT * WHERE {
         |   GRAPH <$uri> {
         |     { ?p a rdfs:Property }
         |     UNION
         |     { ?p a owl:ObjectProperty }
         |     UNION
         |     { ?p a owl:DatatypeProperty }
         |
         |     OPTIONAL { ?p rdfs:label ?label }
         |     OPTIONAL { ?p rdfs:comment ?desc }
         |     OPTIONAL { ?p rdfs:domain ?domain }
         |     OPTIONAL { ?p rdfs:range ?range }
         |   }
         | }
      """.stripMargin

    val classMap = classes.map(c => (c.info.uri, c)).toMap
    def getClass(uri: String) = classMap.getOrElse(uri, VocabularyClass(Info(uri)))

    for(result <- endpoint.select(propertyQuery).bindings) yield {
      val info =
        Info(
          uri = result("p").value,
          label = result.get("label").map(_.value),
          description = result.get("desc").map(_.value)
        )
      VocabularyProperty(
        info = info,
        domain = result.get("domain").map(_.value).map(getClass),
        range = result.get("range").map(_.value).map(getClass)
      )
    }
  }

}
