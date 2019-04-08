package org.silkframework.plugins.dataset.xml

import org.scalatest.{FlatSpec, Matchers}
import org.silkframework.dataset.DataSource
import org.silkframework.entity._
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource.ClasspathResourceLoader
import org.silkframework.util.Uri

import scala.languageFeature.postfixOps

//noinspection ScalaStyle
abstract class XmlSourceTestBase extends FlatSpec with Matchers {

  implicit val userContext: UserContext = UserContext.Empty
  def xmlSource(name: String, uriPattern: String): DataSource with XmlSourceTrait

  behavior of "XML Dataset"

  personTests("persons.xml")
  personTests("persons_unformatted.xml")

  def personTests(fileName: String): Unit = {

    val persons = XmlDoc(fileName)

    it should s"read the root element, if the base path is empty. ($fileName)" in {
      (persons atPath "").uris shouldBe Seq("Persons")
    }

    it should s"read the direct children, if they are referenced by a direct path ($fileName)" in {
      (persons atPath "Person").uris shouldBe Seq("Person", "Person")
      (persons atPath "/Person").uris shouldBe Seq("Person", "Person")
    }

    it should s"extract values of direct children ($fileName)" in {
      (persons atPath "Person" valuesAt "Name") shouldBe Seq(Seq("Max Doe"), Seq("Max Noe"))
    }

    it should s"extract values of indirect children ($fileName)" in {
      (persons atPath "Person" valuesAt "Events/Birth") shouldBe Seq(Seq("May 1900"), Seq())
      (persons atPath "Person" valuesAt "Events/Death") shouldBe Seq(Seq("June 1990"), Seq())
    }

    it should s"extract values of attributes ($fileName)" in {
      (persons atPath "Person" valuesAt "Events/@count") shouldBe Seq(Seq("2"), Seq())
    }

    it should s"return no value if a tag is missing ($fileName)" in {
      (persons atPath "Person" valuesAt "MissingTag") shouldBe Seq(Seq(), Seq())
    }

    it should s"return no value if an attribute is missing ($fileName)" in {
      (persons atPath "Person" valuesAt "@MissingAttribute") shouldBe Seq(Seq(), Seq())
    }

    it should s"support property filters ($fileName)" in {
      (persons atPath "Person" valuesAt "Properties/Property[Key = \"2\"]/Value") shouldBe Seq(Seq("V2"), Seq())
    }

    it should s"allow wildcard * path elements ($fileName)" in {
      (persons atPath "Person" valuesAt "Events/*") shouldBe Seq(Seq("May 1900", "June 1990"), Seq())
    }

    it should s"allow wildcard ** path elements ($fileName)" in {
      (persons atPath "" valuesAt "Person/**/Value") shouldBe Seq(Seq("V1", "V2", "V3"))
    }

    it should s"generate unique IDs ($fileName)" in {
      val directIds = persons atPath "Person" valuesAt "#id"
      val eventIds = persons atPath "Person" valuesAt "Events/#id"
      val allIds = directIds ++ eventIds

      allIds.distinct shouldBe allIds
    }

    it should s"allow retrieving the text of a selected element ($fileName)" in {
      (persons atPath "Person/ID" valuesAt "#text") shouldBe Seq(Seq("1"), Seq("2"))
    }

    it should s"generate correct URIs for non-leaf nodes when the URI pattern is empty ($fileName)" in {
      (persons withUriPattern "" atPath "Person" entityURIsAt "Properties/Property").head shouldBe
        (persons withUriPattern "" atPath "Person/Properties/Property").uris
    }

    it should s"generate correct URIs for non-leaf nodes when the URI pattern is defined ($fileName)" in {
      (persons withUriPattern "http://example.org/Property{Value}" atPath "Person" valuesAt "Properties/Property") shouldBe
        Seq(Seq("http://example.org/PropertyV1", "http://example.org/PropertyV2", "http://example.org/PropertyV3"), Seq())
    }

    it should s"list all base paths as types ($fileName)" in {
      persons.types shouldBe
        Seq(
          "",
          "Person",
          "Person/Events",
          "Person/Properties",
          "Person/Properties/Property",
          "Person/Properties/Property/Key")
    }

    it should s"list all paths of the root node ($fileName)" in {
      (persons atPath "").subPaths shouldBe
        Seq("Person", "Person/ID", "Person/Name", "Person/Events", "Person/Events/@count", "Person/Events/Birth",
          "Person/Events/Death", "Person/Properties", "Person/Properties/Property", "Person/Properties/Property/Key",
          "Person/Properties/Property/Key/@id", "Person/Properties/Property/Value")
    }

    it should s"list all paths of the root node of depth 1 ($fileName)" in {
      (persons atPath "").subPathsDepth(1) shouldBe
        Seq("Person")
    }

    it should s"list all paths of the root node of depth 2 ($fileName)" in {
      (persons atPath "").subPathsDepth(2) shouldBe
        Seq("Person", "Person/ID", "Person/Name", "Person/Events", "Person/Properties")
    }

    it should s"list all paths, given a base path ($fileName)" in {
      (persons atPath "Person").subPaths shouldBe
        Seq("ID", "Name", "Events", "Events/@count", "Events/Birth", "Events/Death", "Properties",
          "Properties/Property", "Properties/Property/Key", "Properties/Property/Key/@id", "Properties/Property/Value")
    }

    it should s"list all paths of depth 1, given a base path ($fileName)" in {
      (persons atPath "Person").subPathsDepth(1) shouldBe
        Seq("ID", "Name", "Events", "Properties")
    }

    it should s"list all paths of depth 2, given a base path ($fileName)" in {
      (persons atPath "Person").subPathsDepth(2) shouldBe
        Seq("ID", "Name", "Events", "Events/@count", "Events/Birth", "Events/Death", "Properties",
          "Properties/Property")
    }

    it should s"list all leaf paths of the root ($fileName)" in {
      (persons atPath "").leafPaths(Int.MaxValue) shouldBe
        Seq("Person/ID", "Person/Name", "Person/Events/@count", "Person/Events/Birth", "Person/Events/Death",
          "Person/Properties/Property/Key", "Person/Properties/Property/Key/@id", "Person/Properties/Property/Value")
    }

    it should s"list all leaf paths of a subpath ($fileName)" in {
      (persons atPath "Person/Properties").leafPaths(Int.MaxValue) shouldBe
        Seq("Property/Key", "Property/Key/@id", "Property/Value")
    }


    it should s"respect the limit when reading entities ($fileName)" in {
      (persons atPath "Person" limit 1 valuesAt "Name") shouldBe Seq(Seq("Max Doe"))
      (persons atPath "Person" limit 2 valuesAt "Name") shouldBe Seq(Seq("Max Doe"), Seq("Max Noe"))
    }

    it should s"allow the retrieval of single entities ($fileName)" in {
      (persons atPath "Person" entityWithUri "Person").uri shouldBe Uri("Person")
    }
  }

  //TODO TypedPath change:  is the difference between String and Uri type so important for Xml?
  it should "retrieve typed paths" in {
    xmlSource("persons.xml", "").retrievePaths("Person").map(tp => tp.normalizedSerialization -> tp.valueType -> tp.isAttribute) shouldBe IndexedSeq(
      "ID" -> StringValueType -> false,
      "Name" -> StringValueType -> false,
      "Events" -> UriValueType -> false,
      "Events/@count" -> StringValueType -> true,
      "Events/Birth" -> StringValueType -> false,
      "Events/Death" -> StringValueType -> false,
      "Properties" -> UriValueType -> false,
      "Properties/Property" -> UriValueType -> false,
      "Properties/Property/Key" -> UriValueType -> false,
      "Properties/Property/Key/@id" -> StringValueType -> true,
      "Properties/Property/Value" -> StringValueType -> false
    )
  }


  /**
    * References an XML document from the test resources.
    */
  case class XmlDoc(name: String, uriPattern: String = "{#tag}") {

    private val resourceLoader = ClasspathResourceLoader("org/silkframework/plugins/dataset/xml")

    private val source = xmlSource(name, uriPattern)

    def withUriPattern(pattern: String): XmlDoc = {
      copy(uriPattern = pattern)
    }

    def atPath(basePath: String = ""): Entities = {
      Entities(source, basePath)
    }

    def types: Seq[String] = {
      source.retrieveTypes().map(_._1).toSeq
    }

  }

  /**
    * References entities in a specified XML document at a specified path.
    */
  case class Entities(xmlSource: DataSource with XmlSourceTrait, basePath: String, entityLimit: Option[Int] = None) {

    def limit(maxCount: Int): Entities = {
      copy(entityLimit = Some(maxCount))
    }

    def uris: Seq[String] = {
      retrieve(IndexedSeq.empty).map(_.uri.toString)
    }

    def valuesAt(pathStr: String): Seq[Seq[String]] = {
      val path = Path.parse(pathStr).asStringTypedPath
      retrieve(IndexedSeq(path)).map(_.evaluate(path))
    }

    def entityWithUri(uri: Uri): Entity = {
      val entityDesc =
        EntitySchema(
          typeUri = Uri(basePath),
          typedPaths = IndexedSeq.empty
        )
      xmlSource.retrieveByUri(entityDesc, Seq(uri)).head
    }

    def entityURIsAt(pathStr: String): Seq[Seq[String]] = {
      val path = TypedPath(Path.parse(pathStr), UriValueType, isAttribute = false)
      retrieve(IndexedSeq(path)).map(_.evaluate(path))
    }

    def subPaths: Seq[String] = {
      xmlSource.retrievePaths(basePath, depth = Int.MaxValue).map(_.serialize())
    }

    def subPathsDepth(depth: Int): Seq[String] = {
      xmlSource.retrievePaths(basePath, depth = depth).map(_.serialize())
    }

    def leafPaths(depth: Int): Seq[String] = {
      xmlSource.retrieveXmlPaths(basePath, depth, None, onlyLeafNodes = true, onlyInnerNodes = false).map(_.normalizedSerialization)
    }

    private def retrieve(paths: IndexedSeq[TypedPath]): Seq[Entity] = {
      val entityDesc =
        EntitySchema(
          typeUri = Uri(basePath),
          typedPaths = paths
        )
      xmlSource.retrieve(entityDesc, entityLimit).toSeq
    }

  }

}
