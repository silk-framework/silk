package controllers.util

import org.apache.jena.rdf.model.{Model, ModelFactory}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

/**
  * Tests [[ProjectUtils.connectedSubgraph]], in particular that it assembles a complete record from a
  * root seed regardless of whether the linking triples point forward (root -> prop -> child) or backward
  * (child -> prop -> root, as written for backward/inverse property mappings).
  */
class ConnectedSubgraphTest extends AnyFlatSpec with Matchers {

  private val person = "http://example.org/person/1"
  private val address = "http://example.org/address/1"
  private val label = "http://www.w3.org/2000/01/rdf-schema#label"
  private val hasAddress = "http://example.org/hasAddress"
  private val addressOf = "http://example.org/addressOf"
  private val city = "http://example.org/city"
  private val otherPerson = "http://example.org/person/2"

  private def stmt(model: Model, s: String, p: String, o: String): Unit =
    model.add(model.getResource(s), model.getProperty(p), model.getResource(o))

  private def literal(model: Model, s: String, p: String, o: String): Unit =
    model.add(model.getResource(s), model.getProperty(p), o)

  behavior of "ProjectUtils.connectedSubgraph"

  it should "collect a child linked by a forward property" in {
    val model = ModelFactory.createDefaultModel()
    literal(model, person, label, "Alice")
    stmt(model, person, hasAddress, address)   // forward: person -> hasAddress -> address
    literal(model, address, city, "Berlin")

    val result = ProjectUtils.connectedSubgraph(model, Seq(person))
    result.size mustBe 3
    result.contains(result.getResource(address), result.getProperty(city), "Berlin") mustBe true
  }

  it should "collect a child linked by a backward (inverse) property" in {
    val model = ModelFactory.createDefaultModel()
    literal(model, person, label, "Alice")
    stmt(model, address, addressOf, person)    // backward: address -> addressOf -> person
    literal(model, address, city, "Berlin")

    val result = ProjectUtils.connectedSubgraph(model, Seq(person))
    // Without backward traversal this would be just the single label triple.
    result.size mustBe 3
    result.contains(result.getResource(address), result.getProperty(addressOf), result.getResource(person)) mustBe true
    result.contains(result.getResource(address), result.getProperty(city), "Berlin") mustBe true
  }

  it should "not pull in unrelated records" in {
    val model = ModelFactory.createDefaultModel()
    literal(model, person, label, "Alice")
    literal(model, otherPerson, label, "Bob")  // a separate, unconnected record

    val result = ProjectUtils.connectedSubgraph(model, Seq(person))
    result.size mustBe 1
    result.contains(result.getResource(otherPerson), result.getProperty(label), "Bob") mustBe false
  }

  it should "not bleed into another record that shares a referenced entity when roots are given as boundaries" in {
    val book1 = "http://example.org/book/1"
    val book2 = "http://example.org/book/2"
    val author = "http://example.org/person/shared"
    val name = "http://example.org/name"
    val authoredBy = "http://example.org/author"

    val model = ModelFactory.createDefaultModel()
    literal(model, book1, name, "1984")
    stmt(model, book1, authoredBy, author)      // book1 -> author -> shared
    literal(model, book2, name, "Animal Farm")
    stmt(model, book2, authoredBy, author)      // book2 -> author -> shared (same author)
    literal(model, author, label, "Orwell")

    val result = ProjectUtils.connectedSubgraph(model, Seq(book1), roots = Set(book1, book2))

    // book1's own triples plus the shared author are kept ...
    result.contains(result.getResource(book1), result.getProperty(name), "1984") mustBe true
    result.contains(result.getResource(book1), result.getProperty(authoredBy), result.getResource(author)) mustBe true
    result.contains(result.getResource(author), result.getProperty(label), "Orwell") mustBe true
    // ... but nothing from book2 bleeds in, not even the dangling link to the shared author.
    result.contains(result.getResource(book2), result.getProperty(authoredBy), result.getResource(author)) mustBe false
    result.contains(result.getResource(book2), result.getProperty(name), "Animal Farm") mustBe false
  }

  it should "still return the full connected component when no root boundaries are given" in {
    val book1 = "http://example.org/book/1"
    val book2 = "http://example.org/book/2"
    val author = "http://example.org/person/shared"
    val authoredBy = "http://example.org/author"

    val model = ModelFactory.createDefaultModel()
    stmt(model, book1, authoredBy, author)
    stmt(model, book2, authoredBy, author)

    // Without roots the legacy behaviour is preserved: the shared author still links the two books.
    val result = ProjectUtils.connectedSubgraph(model, Seq(book1))
    result.contains(result.getResource(book2), result.getProperty(authoredBy), result.getResource(author)) mustBe true
  }
}
