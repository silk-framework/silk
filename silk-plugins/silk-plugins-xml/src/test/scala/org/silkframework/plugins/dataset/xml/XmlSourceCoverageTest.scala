package org.silkframework.plugins.dataset.xml

import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.config.Prefixes
import org.silkframework.dataset.CoveragePathInput
import org.silkframework.entity.{Entity, EntitySchema, Path}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource.ClasspathResourceLoader
import org.silkframework.util.Uri

/**
  * Tests for XmlSource
  */
class XmlSourceCoverageTest extends FlatSpec with MustMatchers {
  behavior of "XML Source"

  implicit val prefixes: Prefixes = Prefixes(Map.empty)
  implicit val userContext: UserContext = UserContext.Empty

  it should "return 0% mapping coverage if there is no mapping" in {
    val source = xmlSource
    val result = source.pathCoverage(Seq(
      CoveragePathInput("Person/Properties/Property", Seq())
    ))
    result.paths.size mustBe 12
    result.paths.forall(!_.covered) mustBe true
  }

  it should "return correct mapping coverage if there are inputs" in {
    val source = xmlSource
    val result = source.pathCoverage(Seq(
      CoveragePathInput("Person/Properties/Property", paths("Key")),
      CoveragePathInput("", paths("""Person[ID="1"]/Name""", "Person/Events/@count"))
    ))
    result.paths.size mustBe 12
    result.paths.count(_.covered) mustBe 3
    result.paths.count(_.fully) mustBe 2
  }

  private def xmlSource: XmlSourceStreaming = {
    val resources = ClasspathResourceLoader("org/silkframework/plugins/dataset/xml/")
    val source = new XmlSourceStreaming(resources.get("persons.xml"), "", "#id")
    source
  }

  it should "return correct value coverage" in {
    val source = xmlSource
    implicit val prefixes = Prefixes(Map.empty)
    val result = source.valueCoverage(Path.parse("Person/Name"), paths("""Person[ID="1"]/Name"""))
    result.overallValues mustBe 2
    result.coveredValues mustBe 1
    val missedValue = result.missedValues.head
    missedValue.value mustBe "Max Noe"
    missedValue.nodeId mustBe defined
  }

  it should "return correct value coverage for more complicated path" in {
    val source = xmlSource
    implicit val prefixes = Prefixes(Map.empty)
    val result = source.valueCoverage(Path.parse("Person/Properties/Property/Value"), paths("""Person/Properties/Property[Key!="2"]/Value"""))
    result.overallValues mustBe 3
    result.coveredValues mustBe 2
    result.missedValues.size mustBe 1
    val missedValue = result.missedValues.head
    missedValue.value mustBe "V2"
    missedValue.nodeId mustBe defined
  }

  it should "return correct value coverage for '*' and '**' paths" in {
    val source = xmlSource
    implicit val prefixes = Prefixes(Map.empty)
    val result = source.valueCoverage(
      Path.parse("Person/Properties/Property/Value"),
      paths("""Person/*/Property[Key="2"]/Value""", """**/Property[Key="1"]/Value"""))
    result.overallValues mustBe 3
    result.coveredValues mustBe 2
    result.missedValues.size mustBe 1
    val missedValue = result.missedValues.head
    missedValue.value mustBe "V3"
    missedValue.nodeId mustBe defined
  }

  it should "match paths with '*' in it" in {
    implicit val source = xmlSource
    matchPath("/Person/Properties/Property/Value", "/Person/Properties/Property/*")
    matchPath("/Person/Properties/Property/Value", "/Person/Properties/*/Value")
    matchPath("/Person/Properties/Property/Value", "/Person/*/Property/Value")
    matchPath("/Person/Properties/Property/Value", "/*/Properties/Property/Value")
    matchPath("/Person/Properties/Property/Value", "/*/Properties/*/Value")
    doNotMatchPath("/Person/Properties/Property/Value", "/*/*/*")
    doNotMatchPath("/Person/Properties/Property/Value", "/*/*/Value")
    doNotMatchPath("/Person/Properties/Property/Value", "/Person/*/Value")
    doNotMatchPath("/Person/Properties/Property/Value", "/Person/Properties/*")
  }

  it should "match paths with '**' in it" in {
    implicit val source = xmlSource
    matchPath("Person/Properties/Property/Value", "/**/Value")
    matchPath("/Person/Properties/Property/Value", "/Person/**/Value")
    matchPath("/Person/Properties/Property/Value", "/**")
    matchPath("/Person/Properties/Property/Value", "/**/Properties/**")
    doNotMatchPath("/Person/Properties/Property/Value", "/Company/**/Value")
    doNotMatchPath("/Person/Properties/Property/Value", "/**/NotThere/Value")
    doNotMatchPath("/Person/Properties/Property/Value", "/Company/**")
    doNotMatchPath("/Person/Properties/Property/Value", "/Person/NotThere/**")
  }

  it should "return peak result" in {
    val result = xmlSource.peak(EntitySchema(Uri(""), typedPaths = IndexedSeq(Path.parse("/Person/Properties/Property/Value").asStringTypedPath)), 3).toSeq
    result.size mustBe 1
    result.head.values mustBe IndexedSeq(Seq("V1", "V2", "V3"))
  }

  it should "return peak results with sub path set" in {
    val result = xmlSource.peak(EntitySchema(Uri(""), typedPaths = IndexedSeq(Path.parse("/Value").asStringTypedPath),
      subPath = Path.parse("/Person/Properties/Property")), 3).toSeq
    result.size mustBe 3
    result.map(_.values) mustBe Seq(IndexedSeq(Seq("V1")), IndexedSeq(Seq("V2")), IndexedSeq(Seq("V3")))
  }

  private def matchPath(sourcePath: String,
                        inputPath: String,
                        typePath: String = "")
                       (implicit xmlSource: XmlSourceStreaming): Unit = {
    assert(xmlSource.matchPath(typePath, path(inputPath), path(sourcePath)), s"$sourcePath did not match $inputPath with type '$typePath'")
  }

  private def doNotMatchPath(sourcePath: String,
                             inputPath: String,
                             typePath: String = "")
                            (implicit xmlSource: XmlSourceStreaming): Unit = {
    assert(!xmlSource.matchPath(typePath, path(inputPath), path(sourcePath)), s"$sourcePath did match $inputPath with type '$typePath'")
  }

  private def path(pathStr: String): Path = Path.parse(pathStr)

  private def paths(paths: String*): Seq[Path] = {
    paths map path
  }
}
