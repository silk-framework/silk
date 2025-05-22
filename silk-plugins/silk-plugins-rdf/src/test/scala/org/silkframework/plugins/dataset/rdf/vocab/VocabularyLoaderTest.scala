package org.silkframework.plugins.dataset.rdf.vocab

import org.apache.jena.query.DatasetFactory
import org.apache.jena.rdf.model.ModelFactory

import org.silkframework.plugins.dataset.rdf.endpoint.JenaDatasetEndpoint
import org.silkframework.rule.vocab._
import org.silkframework.runtime.activity.UserContext
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class VocabularyLoaderTest extends AnyFlatSpec with Matchers {
  private implicit val userContext: UserContext = UserContext.Empty
  private val MOVIE = "Movie"
  private val FILM = "Film"
  private val PERSON = "Person"
  private val HAS_DIRECTOR = "hasDirector"

  behavior of "VocabularyLoader"

  val graphUri = "urn:example"

  private lazy val loader = load("vocabulary.ttl")

  lazy val vocabulary: Option[Vocabulary] = loader.retrieveVocabulary(graphUri)
  lazy val classes: Seq[VocabularyClass] = loader.retrieveClasses(graphUri).toSeq.sortBy(_.info.uri)
  lazy val classMap: Map[String, VocabularyClass] = classes.map(c => (c.info.uri, c)).toMap
  lazy val properties: Seq[VocabularyProperty] = loader.retrieveProperties(graphUri, classes).toSeq.sortBy(_.info.uri)

  it should "load the label of the vocabulary" in {
    vocabulary shouldBe defined
    val info = vocabulary.get.info
    info.label shouldBe Some("Test Ontology")
    info.description shouldBe Some("Test Ontology description")
    info.altLabels shouldBe Seq("Alternative test ontology label")
  }

  it should "load classes" in {
    classes.size shouldBe 3
    classes(1) shouldBe VocabularyClass(GenericInfo(uri(MOVIE), Some(MOVIE), None, Seq.empty, Some("urn:example")), Seq())
    classes(2) shouldBe VocabularyClass(GenericInfo(uri(PERSON), Some(PERSON), Some("A Person"), Seq.empty, Some("urn:example")), Seq())
  }

  it should "pick the right language" in {
    classes.head shouldBe VocabularyClass(GenericInfo(uri("Employee"), Some("Angestellter"), Some("Employee of a company"), Seq.empty, Some("urn:example")), Seq(uri(PERSON)))
    val hasDirector = properties.find(_.info.uri == uri(HAS_DIRECTOR)).get
    hasDirector.info.label shouldBe Some("director5")
    hasDirector.info.description shouldBe Some("Director of a movie")
  }

  it should "load properties" in {
    properties.size shouldBe 3
    properties.head shouldBe
      VocabularyProperty(
        info = GenericInfo(uri("hasDate"), Some("release date"), None, Seq.empty, Some("urn:example")),
        domain = Some(classMap(uri(MOVIE))),
        range = None,
        propertyType = DatatypePropertyType
      )
    properties(1) shouldBe
      VocabularyProperty(
        info = GenericInfo(uri("hasDirector"), Some("director5"), Some("Director of a movie"), Seq.empty, Some("urn:example")),
        domain = Some(classMap(uri(MOVIE))),
        range = Some(classMap(uri(PERSON))),
        propertyType = ObjectPropertyType
      )
  }

  it should "load all kinds of alternative labels and comments" in {
    properties(2) shouldBe
        VocabularyProperty(
          info = GenericInfo(uri("label"), Some("label"), Some("dc description"), Seq("dc identifier", "dc title", "dct identifier", "foaf name", "pref label", "skos notation"), Some("urn:example")),
          domain = Some(classMap(uri(MOVIE))),
          range = Some(VocabularyClass(GenericInfo("http://www.w3.org/2001/XMLSchema#string"), Seq())),
          propertyType = DatatypePropertyType
        )
  }

  private def load(resource: String): VocabularyLoader = {
    // Load example into Jena model
    val stream = getClass.getClassLoader.getResourceAsStream("org/silkframework/plugins/dataset/rdf/vocab/" + resource)
    val model = ModelFactory.createDefaultModel()
    model.read(stream, null, "TURTLE")

    // Create Jena dataset
    val dataset = DatasetFactory.createTxnMem()
    dataset.addNamedModel(graphUri, model)

    // Create VocabularyLoader
    val endpoint = new JenaDatasetEndpoint(dataset)
    new VocabularyLoader(endpoint)
  }

  private def uri(localName: String): String = "https://silkframework.org/testOntology/" + localName

}
