package org.silkframework.plugins.dataset.rdf.vocab

import com.hp.hpl.jena.query.DatasetFactory
import com.hp.hpl.jena.rdf.model.ModelFactory
import org.scalatest.{FlatSpec, ShouldMatchers}
import org.silkframework.plugins.dataset.rdf.endpoint.JenaDatasetEndpoint
import org.silkframework.rule.vocab.{Info, VocabularyClass, VocabularyProperty}

class VocabularyLoaderTest extends FlatSpec with ShouldMatchers {

  behavior of "VocabularyLoader"

  val graphUri = "urn:example"

  private val loader = load()

  val classes = loader.retrieveClasses(graphUri).toSeq.sortBy(_.info.uri)
  val classMap = classes.map(c => (c.info.uri, c)).toMap

  it should "load classes" in {
    classes.size shouldBe 2
    classes(0) shouldBe VocabularyClass(Info(uri("Movie"), Some("Movie"), None))
    classes(1) shouldBe VocabularyClass(Info(uri("Person"), Some("Person"), Some("A Person")))
  }

  it should "load properties" in {
    val properties = loader.retrieveProperties(graphUri, classes).toSeq.sortBy(_.info.uri)
    properties.size shouldBe 2
    properties(0) shouldBe
      VocabularyProperty(
        info = Info(uri("hasDate"), Some("release date"), None),
        domain = Some(classMap(uri("Movie"))),
        range = None
      )
    properties(1) shouldBe
      VocabularyProperty(
        info = Info(uri("hasDirector"), Some("director"), Some("Director of a movie")),
        domain = Some(classMap(uri("Movie"))),
        range = Some(classMap(uri("Person")))
      )
  }

  private def load(): VocabularyLoader = {
    // Load example into Jena model
    val stream = getClass.getClassLoader.getResourceAsStream("org/silkframework/plugins/dataset/rdf/vocab/vocabulary.ttl")
    val model = ModelFactory.createDefaultModel()
    model.read(stream, null, "TURTLE")

    // Create Jena dataset
    val dataset = DatasetFactory.createMem()
    dataset.addNamedModel(graphUri, model)

    // Create VocabularyLoader
    val endpoint = new JenaDatasetEndpoint(dataset)
    new VocabularyLoader(endpoint)
  }

  private def uri(localName: String): String = "https://silkframework.org/testOntology/" + localName

}
