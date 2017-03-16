package org.silkframework.plugins.dataset.xml

import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.config.Prefixes
import org.silkframework.dataset.CoveragePathInput
import org.silkframework.entity.Path
import org.silkframework.runtime.resource.ClasspathResourceLoader

/**
  * Tests for XmlSource
  */
class XmlSourceTest extends FlatSpec with MustMatchers {
  behavior of "XML Source"

  it should "return 0% mapping coverage if there is no mapping" in {
    val source: XmlSource = xmlSource
    implicit val prefixes = Prefixes(Map.empty)
    val result = source.pathCoverage(Seq(
      CoveragePathInput("Person/Properties/Property", Seq())
    ))
    result.paths.size mustBe 7
    result.paths.forall(!_.covered) mustBe true
  }

  it should "return correct mapping coverage if there are inputs" in {
    val source = xmlSource
    implicit val prefixes = Prefixes(Map.empty)
    val result = source.pathCoverage(Seq(
      CoveragePathInput("Person/Properties/Property", paths("Key")),
      CoveragePathInput("", paths("""Person[ID="1"]/Name""", "Person/Events/@count"))
    ))
    result.paths.size mustBe 7
    result.paths.count(_.covered) mustBe 3
    result.paths.count(_.fully) mustBe 2
  }

  private def xmlSource: XmlSource = {
    val resources = ClasspathResourceLoader("org/silkframework/plugins/dataset/xml/")
    val source = new XmlSource(resources.get("persons.xml"), "", "#id")
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

  it should "return correct value coverage second" in {
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

  private def paths(paths: String*): Seq[Path] = {
    paths.map(str => Path.parse(str))
  }
}
