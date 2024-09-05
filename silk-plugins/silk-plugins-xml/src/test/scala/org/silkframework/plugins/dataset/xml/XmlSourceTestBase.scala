package org.silkframework.plugins.dataset.xml


import org.silkframework.config.Prefixes
import org.silkframework.dataset.DataSource
import org.silkframework.entity._
import org.silkframework.entity.paths.{TypedPath, UntypedPath}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource.ClasspathResourceLoader
import org.silkframework.util.Uri

import scala.languageFeature.postfixOps
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.collection.immutable.Seq

//noinspection ScalaStyle
abstract class XmlSourceTestBase extends AnyFlatSpec with Matchers {

  implicit protected val userContext: UserContext = UserContext.Empty
  implicit protected val prefixes: Prefixes = Prefixes.empty

  def xmlSource(name: String, uriPattern: String, baseType: String = ""): DataSource with XmlSourceTrait
  // Some operations are not supported in streaming mode
  def isStreaming: Boolean

  behavior of "XML Dataset"

  personTests("persons.xml", isFormatted = true)
  personTests("persons_unformatted.xml", isFormatted = false)

  def personTests(fileName: String, isFormatted: Boolean): Unit = {

    val persons = XmlDoc(fileName)

    it should s"read the root element, if the base path is empty. ($fileName)" in {
      (persons atPath "").uris shouldBe Seq("Persons")
      persons atPath "" valuesAt "@rootAttribute" shouldBe Seq(Seq("top"))
    }

    it should s"read the direct children, if they are referenced by a direct path ($fileName)" in {
      (persons atPath "Person").uris shouldBe Seq("Person", "Person")
      (persons atPath "/Person").uris shouldBe Seq("Person", "Person")
    }

    it should s"extract values of direct children ($fileName)" in {
      (persons atPath "Person" valuesAt "Name") shouldBe Seq(Seq("Max Doe"), Seq("Max Noe"))
    }

    it should s"find start element of paths where the path contains optional elements ($fileName)" in {
      (persons atPath "Person/OnlyInSecondPerson" valuesAt "#text") shouldBe Seq(Seq("only"))
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

    it should s"return no value for paths that include non-existing path segments ($fileName)" in {
      (persons atPath "Person" valuesAt "NonExisting/Name") shouldBe Seq(Seq(), Seq())
      (persons atPath "Person" valuesAt "NonExisting/Property/Key") shouldBe Seq(Seq(), Seq())
      (persons atPath "Person/NonExisting/Events" valuesAt "Birth") shouldBe Seq()
      (persons atPath "Person/NonExisting/Birth" valuesAt "#text") shouldBe Seq()
    }

    it should s"return no value for paths that have gaps in their paths ($fileName)" in {
      (persons atPath "Person/Birth" valuesAt "#text") shouldBe Seq()
      (persons atPath "Person" valuesAt "Birth") shouldBe Seq(Seq(), Seq())
    }

    it should s"return no value if an attribute is missing ($fileName)" in {
      (persons atPath "Person" valuesAt "@MissingAttribute") shouldBe Seq(Seq(), Seq())
    }

    it should s"support property filters ($fileName)" in {
      (persons atPath "Person" valuesAt "Properties/Property[Key = \"2\"]/Value") shouldBe Seq(Seq("V2"), Seq())
      if(!isStreaming) {
        // FIXME: Filtering in object path in streaming mode not allowed on element values, only attributes.
        (persons atPath "Person/Properties/Property[Key = \"2\"]" valuesAt "Value") shouldBe Seq(Seq("V2"))
      }
    }

    it should s"support property filters on attributes ($fileName)" in {
      (persons atPath "Person" valuesAt "Events[@count = \"2\"]/Birth") shouldBe Seq(Seq("May 1900"), Seq())
      (persons atPath "Person" valuesAt "Events[@count = \"3\"]/Birth") shouldBe Seq(Seq(), Seq())
      if(!isStreaming) {
        // FIXME: No backward paths supported in streaming mode
        (persons atPath "Person/Properties" valuesAt "Property/Key[@id = \"id3\"]\\../Value") shouldBe Seq(Seq("V2"))
        (persons atPath "Person/Properties" valuesAt "Property/Key[@id = \"id3\"]\\../Value") shouldBe Seq(Seq("V2"))
      }
      (persons atPath "Person/Properties" valuesAt "Property/Key[@id = \"id3\"]/#text") shouldBe Seq(Seq("2"))
      (persons atPath "Person/Properties" valuesAt "Property/Key[@id = \"NO_ID\"]/#text") shouldBe Seq(Seq())
      (persons atPath "Person/Properties/Property/Key[@id = \"id3\"]" valuesAt "#text") shouldBe Seq(Seq("2"))
      (persons atPath "Person/Events[@count = \"2\"]" valuesAt "Birth") shouldBe Seq(Seq("May 1900"))
      (persons atPath "Person/Events[@count = \"3\"]" valuesAt "Birth") shouldBe Seq()
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
      (persons atPath "Person" valuesAt "Properties/Property/Key/#text") shouldBe Seq(Seq("1", "2", ""), Seq())
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
        Seq("@rootAttribute", "Person", "Person/ID", "Person/Name", "Person/OnlyInSecondPerson", "Person/Events", "Person/Events/@count", "Person/Events/Birth",
          "Person/Events/Death", "Person/Properties", "Person/Properties/Property", "Person/Properties/Property/Key",
          "Person/Properties/Property/Value", "Person/Properties/Property/Key", "Person/Properties/Property/Key/@id")
    }

    it should s"list all paths of the root node of depth 1 ($fileName)" in {
      (persons atPath "").subPathsDepth(1) shouldBe
        Seq("@rootAttribute", "Person")
    }

    it should s"list all paths of the root node of depth 2 ($fileName)" in {
      (persons atPath "").subPathsDepth(2) shouldBe
        Seq("@rootAttribute", "Person", "Person/ID", "Person/Name", "Person/OnlyInSecondPerson", "Person/Events", "Person/Properties")
    }

    it should s"list all paths, given a base path ($fileName)" in {
      (persons atPath "Person").subPaths shouldBe
        Seq("ID", "Name", "OnlyInSecondPerson", "Events", "Events/@count", "Events/Birth", "Events/Death", "Properties",
          "Properties/Property", "Properties/Property/Key", "Properties/Property/Value", "Properties/Property/Key",
          "Properties/Property/Key/@id")
    }

    it should s"list all paths of depth 1, given a base path ($fileName)" in {
      (persons atPath "Person").subPathsDepth(1) shouldBe
        Seq("ID", "Name", "OnlyInSecondPerson", "Events", "Properties")
    }

    it should s"list all paths of depth 2, given a base path ($fileName)" in {
      (persons atPath "Person").subPathsDepth(2) shouldBe
        Seq("ID", "Name", "OnlyInSecondPerson", "Events", "Events/@count", "Events/Birth", "Events/Death", "Properties",
          "Properties/Property")
    }

    it should s"retrieve paths when the asterisk operator is used in the base path ($fileName)" in {
      (XmlDoc("persons.xml") atPath "*/Properties/*").subPaths shouldBe Seq("Key", "Value", "Key", "Key/@id")
      (XmlDoc("persons.xml") atPath "Person/*/Property").subPaths shouldBe Seq("Key", "Value", "Key", "Key/@id")
    }

    it should s"respect the limit when reading entities ($fileName)" in {
      (persons atPath "Person" limit 1 valuesAt "Name") shouldBe Seq(Seq("Max Doe"))
      (persons atPath "Person" limit 2 valuesAt "Name") shouldBe Seq(Seq("Max Doe"), Seq("Max Noe"))
    }

    it should s"allow the retrieval of single entities ($fileName)" in {
      (persons atPath "Person" entityWithUri "Person").uri shouldBe Uri("Person")
    }

    it should s"support retrieving the source position (#line and #column) ($fileName)" in {
      if(isFormatted) {
        (persons atPath "Person/ID" valuesAt "#line") shouldBe Seq(Seq("3"), Seq("25"))
        (persons atPath "Person/ID" valuesAt "#column") shouldBe Seq(Seq("12"), Seq("12"))
        (persons atPath "Person" valuesAt "Properties/Property/Key/#line") shouldBe Seq(Seq("11", "15", "19"), Seq())
        (persons atPath "Person" valuesAt "Properties/Property/Key/#column") shouldBe Seq(Seq("14", "26", "15"), Seq())
      } else {
        (persons atPath "Person/ID" valuesAt "#line") shouldBe Seq(Seq("1"), Seq("9"))
        (persons atPath "Person/ID" valuesAt "#column") shouldBe Seq(Seq("42"), Seq("26"))
        (persons atPath "Person" valuesAt "Properties/Property/Key/#line") shouldBe Seq(Seq("6", "6", "6"), Seq())
        (persons atPath "Person" valuesAt "Properties/Property/Key/#column") shouldBe Seq(Seq("32", "91", "147"), Seq())
      }
    }
  }

  it should "read and convert HTML entities" in {
    val xmlEntities = XmlDoc("xmlEntities.xml")
    xmlEntities atPath "" valuesAt "value" shouldBe Seq(Seq("\"quotedValue\""))
  }

  it should "retrieve typed paths" in {
    xmlSource("persons.xml", "").retrievePaths("Person").map(tp => tp.toUntypedPath.normalizedSerialization -> tp.valueType -> tp.isAttribute) shouldBe IndexedSeq(
      "ID" -> ValueType.STRING -> false,
      "Name" -> ValueType.STRING -> false,
      "OnlyInSecondPerson" -> ValueType.STRING -> false,
      "Events" -> ValueType.URI -> false,
      "Events/@count" -> ValueType.STRING -> true,
      "Events/Birth" -> ValueType.STRING -> false,
      "Events/Death" -> ValueType.STRING -> false,
      "Properties" -> ValueType.URI -> false,
      "Properties/Property" -> ValueType.URI -> false,
      "Properties/Property/Key" -> ValueType.STRING ->false,
      "Properties/Property/Value" -> ValueType.STRING -> false,
      "Properties/Property/Key" -> ValueType.URI -> false,
      "Properties/Property/Key/@id" -> ValueType.STRING -> true
    )
    xmlSource("persons.xml", "", "Person").retrievePaths("").map(tp => tp.toUntypedPath.normalizedSerialization -> tp.valueType -> tp.isAttribute) shouldBe IndexedSeq(
      "ID" -> ValueType.STRING -> false,
      "Name" -> ValueType.STRING -> false,
      "OnlyInSecondPerson" -> ValueType.STRING -> false,
      "Events" -> ValueType.URI -> false,
      "Events/@count" -> ValueType.STRING -> true,
      "Events/Birth" -> ValueType.STRING -> false,
      "Events/Death" -> ValueType.STRING -> false,
      "Properties" -> ValueType.URI -> false,
      "Properties/Property" -> ValueType.URI -> false,
      "Properties/Property/Key" -> ValueType.STRING ->false,
      "Properties/Property/Value" -> ValueType.STRING -> false,
      "Properties/Property/Key" -> ValueType.URI -> false,
      "Properties/Property/Key/@id" -> ValueType.STRING -> true
    )
  }

  it should "retrieve paths with base path and type URI set" in {
    val xmlEntities = XmlDoc("persons.xml", basePath = "Person")
    xmlEntities atPath("Events") valuesAt("Birth") shouldBe Seq(Seq("May 1900"))
  }

  it should "generate default URIs for attribute object paths" in {
    val uris = (XmlDoc("persons.xml", "") atPath "Person/Events/@count").uris
    uris should not be empty
    uris.head should include ("count")
    (XmlDoc("persons.xml") atPath "Person/Events/@count").uris shouldBe Seq("attr_count")
  }

  it should "return the concatenated text for nested elements for the #text operator" in {
    (XmlDoc("persons.xml") atPath "Person" valuesAt "Properties/Property/#text") shouldBe Seq(Seq(
      "\n        1\n        V1\n      ",
      "\n        2\n        V2\n      ",
      "\n        \n        V3\n      "
    ), Seq())
  }

  /**
    * References an XML document from the test resources.
    */
  case class XmlDoc(name: String, uriPattern: String = "{#tag}", basePath: String = "") {

    private val resourceLoader = ClasspathResourceLoader("org/silkframework/plugins/dataset/xml")

    private val source = xmlSource(name, uriPattern, basePath)

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
      val path = UntypedPath.parse(pathStr).asStringTypedPath
      retrieve(IndexedSeq(path)).map(_.evaluate(path))
    }

    def entityWithUri(uri: Uri): Entity = {
      val entityDesc =
        EntitySchema(
          typeUri = Uri(basePath),
          typedPaths = IndexedSeq.empty
        )
      xmlSource.retrieveByUri(entityDesc, Seq(uri)).entities.head
    }

    def entityURIsAt(pathStr: String): Seq[Seq[String]] = {
      val path = TypedPath(UntypedPath.parse(pathStr), ValueType.URI, isAttribute = false)
      retrieve(IndexedSeq(path)).map(_.evaluate(path))
    }

    def subPaths: Seq[String] = {
      typedSubPaths.map(_._1)
    }

    def typedSubPaths: Seq[(String, ValueType)] = {
      val typedPaths = xmlSource.retrievePaths(basePath, depth = Int.MaxValue)
      typedPaths.map(tp => (tp.toUntypedPath.serialize(), tp.valueType))
    }

    def subPathsDepth(depth: Int): Seq[String] = {
      xmlSource.retrievePaths(basePath, depth = depth).map(_.toUntypedPath.serialize())
    }

    private def retrieve(paths: IndexedSeq[TypedPath]): Seq[Entity] = {
      val entityDesc =
        EntitySchema(
          typeUri = Uri(basePath),
          typedPaths = paths
        )
      xmlSource.retrieve(entityDesc, entityLimit).entities.toSeq
    }

  }

}
