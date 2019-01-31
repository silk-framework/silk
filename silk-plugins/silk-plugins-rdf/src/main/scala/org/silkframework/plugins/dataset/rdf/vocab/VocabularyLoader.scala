package org.silkframework.plugins.dataset.rdf.vocab

import org.apache.jena.vocabulary.{OWL, RDF}
import org.silkframework.dataset.rdf._
import org.silkframework.rule.vocab._
import org.silkframework.runtime.activity.UserContext

import scala.collection.immutable.SortedMap

private class VocabularyLoader(endpoint: SparqlEndpoint) {
  final val languageRanking: IndexedSeq[String] = IndexedSeq("en", "de", "es", "fr", "it", "pt")

  def retrieveVocabulary(uri: String)(implicit userContext: UserContext): Option[Vocabulary] = {
    val classes = retrieveClasses(uri)
    Some(Vocabulary(
      info = GenericInfo(uri, None, None, Seq.empty),
      classes = classes,
      properties = retrieveProperties(uri, classes)
    ))
  }

  def genericInfoPropertiesPattern(varName: String): String =
    s"""
      |     # comments
      |     OPTIONAL { ?$varName rdfs:comment ?rdfsComment }
      |     OPTIONAL { ?$varName skos:definition ?skosDefinition }
      |     OPTIONAL { ?$varName dct:description ?dctDescription }
      |     OPTIONAL { ?$varName skos:scopeNote ?scopeNote }
      |     # label
      |     OPTIONAL { ?$varName rdfs:label ?label }
      |     # alternative labels
      |     OPTIONAL { ?$varName skos:altLabel ?skosAltLabel }
      |     OPTIONAL { ?$varName dct:title ?dctTitle }
      |     OPTIONAL { ?$varName skos:prefLabel ?skosPrefLabel }
      |     OPTIONAL { ?$varName dc:identifier ?dcIdentifier }
      |     OPTIONAL { ?$varName dct:identifier ?dctIdentifier }
      |     OPTIONAL { ?$varName foaf:name ?foafName }
      |     OPTIONAL { ?$varName skos:notation ?skosNotation }
    """.stripMargin

  val commentVars: Seq[String] = Seq("rdfsComment", "skosDefinition", "dctDescription", "scopeNote")
  val labelVars: Seq[String] = Seq("label", "skosPrefLabel")
  val altLabelVars: Seq[String] = Seq("skosAltLabel", "dctTitle", "skosPrefLabel", "dcIdentifier", "dctIdentifier", "foafName", "skosNotation")

  val prefixes: String =
    """PREFIX owl: <http://www.w3.org/2002/07/owl#>
      |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
      |PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
      |PREFIX dct: <http://purl.org/dc/terms/>
      |PREFIX dc: <http://purl.org/dc/elements/1.1/>
      |PREFIX foaf: <http://xmlns.com/foaf/0.1/>
      |PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
      |""".stripMargin

  def retrieveClasses(uri: String)
                     (implicit userContext: UserContext): Traversable[VocabularyClass] = {
    val classQuery =
      s"""
         | $prefixes
         |
         | SELECT * WHERE {
         |   GRAPH <$uri> {
         |     { ?c a owl:Class }
         |     UNION
         |     { ?c a rdfs:Class }
         |     OPTIONAL { ?c rdfs:subClassOf ?parent }
         |     ${genericInfoPropertiesPattern("c")}
         |   }
         | }
         | ORDER BY ?c
      """.stripMargin

    val queryResult = endpoint.select(classQuery).bindings
    val resultsPerClass = new SequentialGroup(queryResult)
    for((classUri, bindings) <- resultsPerClass) yield {
      val label = rankValues(labelVars.flatMap(collectObjectNodes(_, bindings))).headOption
      val description = rankValues(commentVars.flatMap(collectObjectNodes(_, bindings))).headOption
      val altLabels = rankValues(altLabelVars.flatMap(collectObjectNodes(_, bindings)))
      val parents = collectValues("parent", bindings)
      VocabularyClass(
        info =
          GenericInfo(
            uri = classUri,
            label = label,
            description = description,
            altLabels = altLabels
          ),
        parentClasses = parents
      )
    }
  }

  private def collectValues(varName: String,
                            bindings: Traversable[SortedMap[String, RdfNode]]): Seq[String] = {
    collectObjectNodes(varName, bindings).map(_.value).distinct
  }

  private def collectObjectNodes(varName: String,
                                 bindings: Traversable[SortedMap[String, RdfNode]]): Seq[RdfNode] = {
    bindings.flatMap(_.get(varName)).toSeq
  }

  private def extractLangRank(langTag: String): (Int, String) = {
    val mainLang = langTag.take(2).toLowerCase()
    val idx = languageRanking.indexOf(mainLang)
    if(idx == -1) {
      (Int.MaxValue, mainLang)
    } else {
      (idx, mainLang)
    }
  }

  private def collectLanguageRankedValueOpt(varName: String,
                                            bindings: Traversable[SortedMap[String, RdfNode]]): Option[String] = {
    collectLanguageRankedValues(varName, bindings).headOption
  }

  private def collectLanguageRankedValues(varName: String,
                                          bindings: Traversable[SortedMap[String, RdfNode]]): Seq[String] = {
    val values = bindings.flatMap(_.get(varName).toSeq).toSeq.distinct
    rankValues(values)
  }

  /** Rank the list of RDF literals by language preference and lexicographically */
  private def rankValues(values: Seq[RdfNode]): Seq[String] = {
    val sortedNodes = values.distinct.sortWith {
      case (l: LanguageLiteral, r: LanguageLiteral) =>
        sortLanguageLiterals(l, r)
      case (_: LanguageLiteral, _) =>
        true
      case (_, _: LanguageLiteral) =>
        false
      case (PlainLiteral(l), PlainLiteral(r)) =>
        l < r
      case (_: PlainLiteral, _) =>
        true
      case (_, _: PlainLiteral) =>
        false
      case (l, r) =>
        l.value < r.value
    }
    sortedNodes.map(_.value)
  }

  private def sortLanguageLiterals(l: LanguageLiteral, r: LanguageLiteral) = {
    val (leftRank, leftLang) = extractLangRank(l.language)
    val (rightRank, rightLang) = extractLangRank(r.language)
    if (leftRank == rightRank) {
      if(leftLang == rightLang) {
        l.value < r.value
      } else {
        leftLang < rightLang
      }
    } else {
      leftRank < rightRank
    }
  }

  class SequentialGroup(bindings: Traversable[SortedMap[String, RdfNode]]) extends Traversable[(String, Traversable[SortedMap[String, RdfNode]])] {

    override def foreach[U](emit: ((String, Traversable[SortedMap[String, RdfNode]])) => U): Unit = {
      var currentUri: Option[String] = None
      var groupedBindings: Vector[SortedMap[String, RdfNode]] = Vector.empty
      for(binding <- bindings if !binding("c").isInstanceOf[BlankNode]) {
        val uri = binding("c").value
        if(!currentUri.contains(uri)) {
          emitIfExists(emit, currentUri, groupedBindings)
          currentUri = Some(uri)
          groupedBindings = Vector.empty
        }
        groupedBindings :+= binding
      }
      emitIfExists(emit, currentUri, groupedBindings)
    }

    private def emitIfExists[U](emit: ((String, Traversable[SortedMap[String, RdfNode]])) => U,
                                currentUri: Option[String],
                                groupedBindings: Vector[SortedMap[String, RdfNode]]) = {
      currentUri foreach { uri =>
        emit((uri, groupedBindings))
      }
    }
  }

  val propertyClasses = Set(
    RDF.Property.getURI,
    OWL.DatatypeProperty.getURI,
    OWL.ObjectProperty.getURI
  )

  def retrieveProperties(uri: String, classes: Traversable[VocabularyClass])
                        (implicit userContext: UserContext): Traversable[VocabularyProperty] = {
    val propertyQuery = propertiesOfClassQuery(uri)

    val classMap = classes.map(c => (c.info.uri, c)).toMap
    def getClass(uri: String) = classMap.getOrElse(uri, VocabularyClass(GenericInfo(uri, altLabels = Seq.empty), Seq()))
    val result = endpoint.select(propertyQuery).bindings
    val propertiesGrouped = result.groupBy(_("p"))

    for((propertyResource, bindings) <- propertiesGrouped if !propertyResource.isInstanceOf[BlankNode]) yield {
      val description = rankValues(commentVars.flatMap(collectObjectNodes(_, bindings))).headOption
      val altLabels = rankValues(altLabelVars.flatMap(collectObjectNodes(_, bindings)))
      val info =
        GenericInfo(
          uri = propertyResource.value,
          label = rankValues(labelVars.flatMap(collectObjectNodes(_, bindings))).headOption,
          description = description,
          altLabels = altLabels
        )
      val classes = bindings.flatMap(_.get("class"))
      val propertyType = classes.toSeq.
          map(_.value).
          filter(propertyClasses). // Only interested in any of the property classes
          map(PropertyType.uriToTypeMap). // convert to PropertyType instance
          sortWith(_.preference > _.preference).headOption.getOrElse(BasePropertyType) // take the most preferred one
      VocabularyProperty(
        info = info,
        domain = firstValue("domain", bindings).map(getClass),
        range = firstValue("range", bindings).map(getClass),
        propertyType = propertyType
      )
    }
  }

  private def propertiesOfClassQuery(uri: String) = {
    s"""
       | $prefixes
       |
       | SELECT * WHERE {
       |   GRAPH <$uri> {
       |     { ?p a rdf:Property }
       |     UNION
       |     { ?p a owl:ObjectProperty }
       |     UNION
       |     { ?p a owl:DatatypeProperty }
       |
       |     ?p a ?class
       |     OPTIONAL { ?p rdfs:domain ?domain }
       |     OPTIONAL { ?p rdfs:range ?range }
       |     ${genericInfoPropertiesPattern("p")}
       |   }
       | }
      """.stripMargin
  }

  private def firstValue(variable: String, bindings: Traversable[SortedMap[String, RdfNode]]): Option[String] = {
    bindings.flatMap(_.get(variable)).headOption.map(_.value)
  }

}