package org.silkframework.plugins.dataset.xml

import org.scalatest.{FlatSpec, Matchers}
import org.silkframework.dataset.DataSource
import org.silkframework.entity.{Entity, EntitySchema, Path}
import org.silkframework.runtime.resource.ClasspathResourceLoader
import org.silkframework.util.Uri

import scala.languageFeature.postfixOps

class XmlDatasetTest extends FlatSpec with Matchers {

  val persons = XmlDoc("persons.xml")

  behavior of "XML Dataset"

  it should "read the root element, if the base path is empty." in {
    (persons atPath "").tags shouldBe Seq("Persons")
  }

  it should "read the direct children, if they are referenced by a direct path" in {
    (persons atPath "Person").tags shouldBe Seq("Person", "Person")
    (persons atPath "/Person").tags shouldBe Seq("Person", "Person")
  }

  it should "extract values of direct children" in {
    (persons atPath "Person" valuesAt "Name") shouldBe Seq(Seq("Max Doe"), Seq("Max Noe"))
  }

  it should "extract values of indirect children" in {
    (persons atPath "Person" valuesAt "Events/Birth") shouldBe Seq(Seq("May 1900"), Seq())
    (persons atPath "Person" valuesAt "Events/Death") shouldBe Seq(Seq("June 1990"), Seq())
  }

  it should "extract values of attributes" in {
    (persons atPath "Person" valuesAt "Events/@count") shouldBe Seq(Seq("2"), Seq())
  }

  it should "return no value if a tag is missing" in {
    (persons atPath "Person" valuesAt "MissingTag") shouldBe Seq(Seq(), Seq())
  }

  it should "return no value if an attribute is missing" in {
    (persons atPath "Person" valuesAt "@MissingAttribute") shouldBe Seq(Seq(), Seq())
  }

  it should "support property filters" in {
    (persons atPath "Person" valuesAt "Properties/Property[Key = \"2\"]/Value") shouldBe Seq(Seq("V2"), Seq())
  }

  it should "allow wildcard * path elements" in {
    (persons atPath "Person" valuesAt "Events/*") shouldBe Seq(Seq("May 1900", "June 1990"), Seq())
  }

  it should "allow wildcard ** path elements" in {
    (persons atPath "" valuesAt "Person/**/Key") shouldBe Seq(Seq("1", "2", "3"))
  }

  it should "generate unique IDs" in {
    val directIds = persons atPath "Person" valuesAt "#id"
    val eventIds = persons atPath "Person" valuesAt "Events/#id"
    val allIds = directIds ++ eventIds

    allIds.distinct shouldBe allIds
  }

  it should "allow retrieving the text of a selected element" in {
    (persons atPath "Person/ID" valuesAt "#text") shouldBe Seq(Seq("1"), Seq("2"))
  }

  it should "list all base paths as types" in {
    persons.types shouldBe
      Seq(
        "",
        "/Person",
        "/Person/ID",
        "/Person/Name",
        "/Person/Events",
        "/Person/Events/@count",
        "/Person/Events/Birth",
        "/Person/Events/Death",
        "/Person/Properties",
        "/Person/Properties/Property",
        "/Person/Properties/Property/Key",
        "/Person/Properties/Property/Value")
  }

  it should "list all paths with leaf nodes, given a base path" in {
    (persons atPath "Person").subPaths shouldBe
      Seq("/ID", "/Name", "/Events/@count", "/Events/Birth", "/Events/Death", "/Properties/Property/Key", "/Properties/Property/Value")
  }


  /**
    * References an XML document from the test resources.
    */
  case class XmlDoc(name: String) {

    private val resourceLoader = ClasspathResourceLoader("org/silkframework/plugins/dataset/xml")

    private val xmlSource = XmlDataset(resourceLoader.get(name), uriPattern = "{#tag}").source

    def atPath(basePath: String = ""): Entities = {
      Entities(xmlSource, basePath)
    }

    def types: Seq[String] = {
      xmlSource.retrieveTypes().map(_._1).toSeq
    }

  }

  /**
    * References entities in a specified XML document at a specified path.
    */
  case class Entities(xmlSource: DataSource, basePath: String) {

    def tags: Seq[String] = {
      retrieve(IndexedSeq.empty).map(_.uri)
    }

    def valuesAt(pathStr: String): Seq[Seq[String]] = {
      val path = Path.parse(pathStr)
      retrieve(IndexedSeq(path)).map(_.evaluate(path))
    }

    def subPaths: Seq[String] = {
      xmlSource.retrievePaths(basePath).map(_.serialize())
    }

    private def retrieve(paths: IndexedSeq[Path]): Seq[Entity] = {
      val entityDesc =
        EntitySchema(
          typeUri = Uri(basePath),
          typedPaths = paths.map(_.asStringTypedPath)
        )
      xmlSource.retrieve(entityDesc).toSeq
    }

  }

}
