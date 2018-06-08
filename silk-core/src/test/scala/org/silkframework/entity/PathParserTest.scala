package org.silkframework.entity

import org.scalatest.{FlatSpec, Matchers}
import org.silkframework.config.Prefixes

class PathParserTest extends FlatSpec with Matchers {

  behavior of "path parser"

  implicit private val prefixes: Prefixes = Prefixes(Map(
    "ex" -> "http://www.example.org/",
    "rdf" -> "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
    "rdfs" -> "http://www.w3.org/2000/01/rdf-schema#",
    "pre1" -> "http://www.pre.cc/pre1/",
    "pre2" -> "http://www.pre.cc/pre1/pre2/",
    "pre3" -> "http://www.pre.cc/pre1/pre3#"
  ))

  private val p = new PathParser(prefixes)

  it should " parse simple forward paths with prefixes" in {
    p.parse("?a/ex:prop") should equal(Path(ForwardOperator("http://www.example.org/prop") :: Nil))
  }

  it should " parse simple forward paths with full URIs" in {
    p.parse("?a/<http://www.example.org/prop>") should equal(Path(ForwardOperator("http://www.example.org/prop") :: Nil))
  }

  it should "parse simple backwards paths" in {
    p.parse("?a\\ex:prop") should equal(Path(BackwardOperator("http://www.example.org/prop") :: Nil))
  }

  it should " parse paths with simplified syntax" in {
    p.parse("ex:prop") should equal(Path(ForwardOperator("http://www.example.org/prop") :: Nil))
    p.parse("/ex:prop") should equal(Path(ForwardOperator("http://www.example.org/prop") :: Nil))
    p.parse("\\ex:prop") should equal(Path(BackwardOperator("http://www.example.org/prop") :: Nil))
  }

  it should " parse empty paths" in {
    p.parse("") should equal(Path(Nil))
  }

  it should "parse chained forward paths" in {
    p.parse("?a/ex:p1\\ex:p2") should equal(Path(ForwardOperator("http://www.example.org/p1") :: BackwardOperator("http://www.example.org/p2") :: Nil))
    p.parse("?a\\ex:p2/ex:p1\\ex:p2") should equal(Path(BackwardOperator("http://www.example.org/p2") :: ForwardOperator("http://www.example.org/p1") :: BackwardOperator("http://www.example.org/p2") :: Nil))
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
}

