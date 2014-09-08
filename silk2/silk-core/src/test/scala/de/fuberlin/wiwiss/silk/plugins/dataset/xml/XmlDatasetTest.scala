package de.fuberlin.wiwiss.silk.plugins.dataset.xml

import de.fuberlin.wiwiss.silk.entity.{EntityDescription, Path, SparqlRestriction}
import de.fuberlin.wiwiss.silk.runtime.resource.ClasspathResourceLoader
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class XmlDatasetTest extends FlatSpec with Matchers {

  val resourceLoader = new ClasspathResourceLoader("xml")

  val personName = Path.parse("?a/<Name>")

  val personBirth = Path.parse("?a/<Events>/<Birth>")

  val personDeath = Path.parse("?a/<Events>/<Death>")

  val entityDesc =
    EntityDescription(
      variable = "a",
      restrictions = SparqlRestriction.empty,
      paths = IndexedSeq(personName, personBirth, personDeath)
    )

  "XmlDatasetTest" should "read direct children of the root element, if the base path is empty." in {
    entities("").size should equal(2)
  }

  "XmlDatasetTest" should "read elements referenced with a base path that includes the root element" in {
    entities("/Persons/Person").size should equal(2)
  }

  "XmlDatasetTest" should "read elements referenced with a base path that does not include the root element" in {
    entities("/Person").size should equal(2)
  }

  "XmlDatasetTest" should "extract person names" in {
    entities()(0).evaluate(personName) should equal(Set("Max Doe"))
    entities()(1).evaluate(personName) should equal(Set("Max Noe"))
  }

  "XmlDatasetTest" should "extract birth and death dates" in {
    entities()(0).evaluate(personBirth) should equal(Set("May 1900"))
    entities()(0).evaluate(personDeath) should equal(Set("June 1990"))
  }

  private def entities(basePath: String = "") = {
    val source = new XmlDataset(resourceLoader.get("persons.xml"), basePath).source
    source.retrieve(entityDesc).toSeq
  }
}
