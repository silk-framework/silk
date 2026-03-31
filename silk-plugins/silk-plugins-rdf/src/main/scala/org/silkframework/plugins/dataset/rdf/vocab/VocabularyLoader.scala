package org.silkframework.plugins.dataset.rdf.vocab

import org.apache.jena.vocabulary.{OWL2, RDF}
import org.silkframework.dataset.rdf._
import org.silkframework.rule.vocab._
import org.silkframework.runtime.activity.UserContext
import org.silkframework.util.Uri

import scala.collection.immutable.SortedMap
import scala.collection.mutable

class VocabularyLoader(endpoint: SparqlEndpoint with GraphStoreTrait) {
  private final val languageRanking: IndexedSeq[String] = IndexedSeq("en", "de", "fr", "es")

  def retrieveVocabulary(uri: String)(implicit userContext: UserContext): Option[Vocabulary] = {
    if(new Uri(uri).isValidUri) {
      val classes = retrieveClasses(uri)
      val vocabGenericInfo = retrieveGenericVocabularyInfo(uri)
      Some(Vocabulary(
        info = vocabGenericInfo,
        classes = classes,
        properties = retrieveProperties(uri, classes),
        endpoint = Some(endpoint)
      ))
    } else {
      None
    }
  }

  private def retrieveGenericVocabularyInfo(vocabularyGraphUri: String)
                                   (implicit userContext: UserContext): GenericInfo = {
    val vocabQuery =
      s"""
         | $prefixes
         |
         | SELECT ?v ?infoProp ?infoValue
         | FROM <$vocabularyGraphUri>
         | WHERE {
         |   { ?v a owl:Ontology }
         |   ${genericInfoPropertiesPattern("v")}
         | }
         | ORDER BY ?v
      """.stripMargin
    val bindings = endpoint.select(vocabQuery).bindings.use(_.toSeq)
    val vocabUri = bindings.flatMap(_.get("v")).headOption.map(_.value).getOrElse(vocabularyGraphUri)
    val valuesByProperty = extractValuesByProperty(bindings)
    extractGenericInfo(vocabUri, valuesByProperty, vocabularyGraphUri)
  }

  private def genericInfoPropertiesPattern(varName: String, additionalProperties: Seq[String] = Seq.empty): String = {
    val allProperties = (baseInfoProperties ++ additionalProperties).distinct
    val propertyFilter = allProperties.map(p => s"<$p>").mkString(", ")
    s"""
      |     OPTIONAL {
      |       ?$varName ?infoProp ?infoValue .
      |       FILTER(?infoProp IN ($propertyFilter))
      |     }
    """.stripMargin
  }

  private val descriptionProperties: Seq[String] = Seq(
    "http://www.w3.org/2000/01/rdf-schema#comment",
    "http://www.w3.org/2004/02/skos/core#definition",
    "http://purl.org/dc/terms/description",
    "http://www.w3.org/2004/02/skos/core#scopeNote"
  )
  private val labelProperties: Seq[String] = Seq(
    "http://www.w3.org/2000/01/rdf-schema#label",
    "http://www.w3.org/2004/02/skos/core#prefLabel"
  )
  private val altLabelProperties: Seq[String] = Seq(
    "http://www.w3.org/2004/02/skos/core#altLabel",
    "http://purl.org/dc/terms/title",
    "http://purl.org/dc/elements/1.1/title",
    "http://www.w3.org/2004/02/skos/core#prefLabel",
    "http://purl.org/dc/elements/1.1/identifier",
    "http://purl.org/dc/terms/identifier",
    "http://xmlns.com/foaf/0.1/name",
    "http://www.w3.org/2004/02/skos/core#notation"
  )
  private val baseInfoProperties: Seq[String] = (descriptionProperties ++ labelProperties ++ altLabelProperties).distinct
  private val subClassOfProperty: String = "http://www.w3.org/2000/01/rdf-schema#subClassOf"
  private val domainProperty: String = "http://www.w3.org/2000/01/rdf-schema#domain"
  private val rangeProperty: String = "http://www.w3.org/2000/01/rdf-schema#range"

  val prefixes: String =
    """PREFIX owl: <http://www.w3.org/2002/07/owl#>
      |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
      |PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
      |PREFIX dct: <http://purl.org/dc/terms/>
      |PREFIX dc: <http://purl.org/dc/elements/1.1/>
      |PREFIX foaf: <http://xmlns.com/foaf/0.1/>
      |PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
      |""".stripMargin

  def retrieveClasses(vocabularyGraphUri: String)
                     (implicit userContext: UserContext): Seq[VocabularyClass] = {
    val classQuery =
      s"""
         | $prefixes
         |
         | SELECT DISTINCT ?c ?infoProp ?infoValue
         | FROM <$vocabularyGraphUri>
         | WHERE {
         |   ?c a ?classType .
         |   FILTER (ISIRI(?c))
         |   FILTER (?classType IN ( rdfs:Class, owl:Class))
         |   ${genericInfoPropertiesPattern("c", Seq(subClassOfProperty))}
         | }
         | ORDER BY ?c
      """.stripMargin

    val resultsPerClass = new SequentialGroup
    endpoint.select(classQuery).bindings.use { queryResult =>
      resultsPerClass.process(queryResult)
    }

    for((classUri, bindings) <- resultsPerClass.result) yield {
      val valuesByProperty = extractValuesByProperty(bindings)
      val parents = getPropertyValues(valuesByProperty, subClassOfProperty)
      VocabularyClass(
        info = extractGenericInfo(classUri, valuesByProperty, vocabularyGraphUri),
        parentClasses = parents
      )
    }
  }

  private def extractValuesByProperty(bindings: Iterable[SortedMap[String, RdfNode]]): Map[String, Seq[RdfNode]] = {
    bindings
      .flatMap { binding =>
        for {
          prop <- binding.get("infoProp")
          value <- binding.get("infoValue")
        } yield (prop.value, value)
      }
      .groupBy(_._1)
      .view.mapValues(_.map(_._2).toSeq)
      .toMap
  }

  private def getPropertyValues(valuesByProperty: Map[String, Seq[RdfNode]], propertyUri: String): Seq[String] = {
    valuesByProperty.getOrElse(propertyUri, Seq.empty).map(_.value).distinct
  }

  private def getFirstPropertyValue(valuesByProperty: Map[String, Seq[RdfNode]], propertyUri: String): Option[String] = {
    valuesByProperty.get(propertyUri).flatMap(_.headOption).map(_.value)
  }

  private def extractGenericInfo(uri: String,
                                 valuesByProperty: Map[String, Seq[RdfNode]],
                                 vocabularyUri: String): GenericInfo = {
    GenericInfo(
      uri = uri,
      label = selectBestValue(valuesByProperty, labelProperties),
      description = selectBestValue(valuesByProperty, descriptionProperties),
      altLabels = rankAllValues(valuesByProperty, altLabelProperties),
      vocabularyUri = Some(vocabularyUri)
    )
  }

  private def rankAllValues(valuesByProperty: Map[String, Seq[RdfNode]],
                            properties: Seq[String]): Seq[String] = {
    val allValues = properties.flatMap(prop => valuesByProperty.getOrElse(prop, Seq.empty))
    rankValues(allValues)
  }

  private def selectBestValue(valuesByProperty: Map[String, Seq[RdfNode]],
                              properties: Seq[String]): Option[String] = {
    rankAllValues(valuesByProperty, properties).headOption
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

  class SequentialGroup {

    private val buffer = mutable.Buffer[(String, Iterable[SortedMap[String, RdfNode]])]()

    def result: Seq[(String, Iterable[SortedMap[String, RdfNode]])] = buffer.toSeq

    def process(bindings: Iterator[SortedMap[String, RdfNode]]): Unit = {
      var currentUri: Option[String] = None
      var groupedBindings: Vector[SortedMap[String, RdfNode]] = Vector.empty
      for(binding <- bindings if !binding("c").isInstanceOf[BlankNode]) {
        val uri = binding("c").value
        if(!currentUri.contains(uri)) {
          emitIfExists(currentUri, groupedBindings)
          currentUri = Some(uri)
          groupedBindings = Vector.empty
        }
        groupedBindings :+= binding
      }
      emitIfExists(currentUri, groupedBindings)
    }

    private def emitIfExists(currentUri: Option[String],
                                groupedBindings: Vector[SortedMap[String, RdfNode]]): Unit = {
      currentUri foreach { uri =>
        buffer.append((uri, groupedBindings))
      }
    }
  }

  val propertyClasses = Set(
    RDF.Property.getURI,
    OWL2.DatatypeProperty.getURI,
    OWL2.ObjectProperty.getURI
  )

  def retrieveProperties(vocabularyUri: String, classes: Iterable[VocabularyClass])
                        (implicit userContext: UserContext): Iterable[VocabularyProperty] = {
    val propertyQuery = propertiesOfClassQuery(vocabularyUri)

    val classMap = classes.map(c => (c.info.uri, c)).toMap
    def getClass(uri: String) = classMap.getOrElse(uri, VocabularyClass(GenericInfo(uri, altLabels = Seq.empty), Seq()))
    val result = endpoint.select(propertyQuery).bindings.use(_.toSeq)
    val propertiesGrouped = result.groupBy(_("p"))

    for((propertyResource, bindings) <- propertiesGrouped if !propertyResource.isInstanceOf[BlankNode]) yield {
      val valuesByProperty = extractValuesByProperty(bindings)
      val info = extractGenericInfo(propertyResource.value, valuesByProperty, vocabularyUri)
      val classes = bindings.flatMap(_.get("class"))
      val propertyType = classes.toSeq
          .map(_.value)
          .filter(propertyClasses)
          .map(PropertyType.uriToTypeMap)
          .sortWith(_.preference > _.preference).headOption.getOrElse(BasePropertyType)
      VocabularyProperty(
        info = info,
        domain = getFirstPropertyValue(valuesByProperty, domainProperty).map(getClass),
        range = getFirstPropertyValue(valuesByProperty, rangeProperty).map(getClass),
        propertyType = propertyType
      )
    }
  }

  private def propertiesOfClassQuery(uri: String) = {
    s"""
       | $prefixes
       |
       | SELECT DISTINCT ?p ?class ?infoProp ?infoValue
       | FROM <$uri>
       | WHERE {
       |   ?p a ?class .
       |   FILTER (?class IN ( rdf:Property, owl:ObjectProperty, owl:DatatypeProperty))
       |   ${genericInfoPropertiesPattern("p", Seq(domainProperty, rangeProperty))}
       | }
      """.stripMargin
  }
}
