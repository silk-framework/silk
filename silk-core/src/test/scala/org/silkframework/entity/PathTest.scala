package org.silkframework.entity

import org.scalatest.{FlatSpec, Matchers}
import org.silkframework.config.Prefixes

class PathTest extends FlatSpec with Matchers {

  behavior of "Path"

  implicit private val prefixes: Prefixes = Prefixes(Map(
    "ex" -> "http://www.example.org/",
    "rdf" -> "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
    "rdfs" -> "http://www.w3.org/2000/01/rdf-schema#",
    "pre1" -> "http://www.pre.cc/pre1/",
    "pre2" -> "http://www.pre.cc/pre1/pre2/",
    "pre3" -> "http://www.pre.cc/pre1/pre3#"
  ))

  private val p = new PathParser(prefixes)

  it should " parse and serialize simple forward paths with prefixes" in {
    parseAndSerialize(
      pathString = "/ex:prop",
      path = Path(ForwardOperator("http://www.example.org/prop") :: Nil),
      normalizedSerialization = "<http://www.example.org/prop>",
      serializationWithPrefixes = "ex:prop"
    )
  }

  it should " parse and serialize simple forward paths with full URIs" in {
    parseAndSerialize(
      pathString = "/<http://www.example.org/prop>",
      path = Path(ForwardOperator("http://www.example.org/prop") :: Nil),
      normalizedSerialization = "<http://www.example.org/prop>",
      serializationWithPrefixes = "ex:prop"
    )
  }

  it should "parse and serialize simple backwards paths" in {
    parseAndSerialize(
      pathString = "\\ex:prop",
      path = Path(BackwardOperator("http://www.example.org/prop") :: Nil),
      normalizedSerialization = "\\<http://www.example.org/prop>",
      serializationWithPrefixes = "\\ex:prop"
    )
  }

  it should " parse paths with simplified syntax" in {
    p.parse("ex:prop") should equal(Path(ForwardOperator("http://www.example.org/prop") :: Nil))
    p.parse("/ex:prop") should equal(Path(ForwardOperator("http://www.example.org/prop") :: Nil))
    p.parse("\\ex:prop") should equal(Path(BackwardOperator("http://www.example.org/prop") :: Nil))
  }

  it should " parse empty paths" in {
    p.parse("") should equal(Path(Nil))
  }

  it should "parse and serialize chained paths" in {
    parseAndSerialize(
      pathString = "/ex:p1\\ex:p2",
      path = Path(ForwardOperator("http://www.example.org/p1") :: BackwardOperator("http://www.example.org/p2") :: Nil),
      normalizedSerialization = "<http://www.example.org/p1>\\<http://www.example.org/p2>",
      serializationWithPrefixes = "ex:p1\\ex:p2"
    )

    parseAndSerialize(
      pathString = "\\ex:p2/ex:p1\\ex:p2",
      path = Path(BackwardOperator("http://www.example.org/p2") :: ForwardOperator("http://www.example.org/p1") :: BackwardOperator("http://www.example.org/p2") :: Nil),
      normalizedSerialization = "\\<http://www.example.org/p2>/<http://www.example.org/p1>\\<http://www.example.org/p2>",
      serializationWithPrefixes = "\\ex:p2/ex:p1\\ex:p2"
    )
  }

  it should "parse property filters with literal values" in {
    val path1 = Path(ForwardOperator("prop") :: PropertyFilter("key", "=", "\"Car\"") :: ForwardOperator("value") :: Nil)
    val path2 = Path(BackwardOperator("http://www.example.org/prop") :: PropertyFilter("http://www.w3.org/2000/01/rdf-schema#label", "=", "\"Car\"") :: Nil)
    val path3 = Path(BackwardOperator("http://www.example.org/prop") :: PropertyFilter("http://www.w3.org/2000/01/rdf-schema#label", "=", "\"Car \"") :: Nil)
    p.parse( """prop[key = "Car"]/value""") should equal(path1)
    p.parse( """?a\ex:prop[rdfs:label = "Car "]""") should equal(path3)
    p.parse( """?a\ex:prop[rdfs:label="Car"]""") should equal(path2)
    p.parse( """?a\ex:prop[rdfs:label="Car "]""") should equal(path3)
  }

  it should "parse property filters with URIs" in {
    p.parse( """?a\ex:prop[rdf:type = ex:Car]""") should equal(Path(BackwardOperator("http://www.example.org/prop") :: PropertyFilter("http://www.w3.org/1999/02/22-rdf-syntax-ns#type", "=", "<http://www.example.org/Car>") :: Nil))
    p.parse( """?a\ex:prop[rdf:type = <http://www.example.org/Car>]""") should equal(Path(BackwardOperator("http://www.example.org/prop") :: PropertyFilter("http://www.w3.org/1999/02/22-rdf-syntax-ns#type", "=", "<http://www.example.org/Car>") :: Nil))
  }

  it should "parse language filters" in {
    p.parse( """?a/ex:prop[@lang = 'en']""") should equal(Path(ForwardOperator("http://www.example.org/prop") :: LanguageFilter("=", "en") :: Nil))
  }

  it should "parse a simplified relative URI forward path" in {
    p.parse("<relative>") shouldBe Path(List(ForwardOperator("relative")))
  }

  it should "parse a path with conflicting prefixes with /" in {
    val path = p.parse("/<http://www.pre.cc/pre1/pre2/test>")
    path shouldBe Path(List(ForwardOperator("http://www.pre.cc/pre1/pre2/test")))
    path.serialize shouldBe "pre2:test"
  }

  it should "parse a path with conflicting prefixes with #" in {
    val path = p.parse("/<http://www.pre.cc/pre1/pre3#test>")
    path shouldBe Path(List(ForwardOperator("http://www.pre.cc/pre1/pre3#test")))
    path.serialize shouldBe "pre3:test"
  }

  def parseAndSerialize(pathString: String, path: Path, normalizedSerialization: String, serializationWithPrefixes: String): Unit = {
    val parsedPath = p.parse(pathString)
    parsedPath shouldBe path
    parsedPath.normalizedSerialization shouldBe normalizedSerialization
    parsedPath.serialize shouldBe serializationWithPrefixes
    // In addition test a round trip with the normalized serialization
    p.parse(normalizedSerialization).normalizedSerialization shouldBe normalizedSerialization
  }
}

