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

  it should "emit a seed's own statement that links to another record's root, but not traverse into that record" in {
    val manager = "http://example.org/manages"
    val salary = "http://example.org/salary"

    val model = ModelFactory.createDefaultModel()
    literal(model, person, label, "Alice")
    stmt(model, person, manager, otherPerson)     // person1 -> manages -> person2 (person2 is another record's root)
    literal(model, otherPerson, label, "Bob")     // person2's own subtree
    literal(model, otherPerson, salary, "100")

    val result = ProjectUtils.connectedSubgraph(model, Seq(person), roots = Set(person, otherPerson))

    // The seed's own outgoing triple to the foreign root is a genuine statement of person1 and must be kept.
    result.contains(result.getResource(person), result.getProperty(label), "Alice") mustBe true
    result.contains(result.getResource(person), result.getProperty(manager), result.getResource(otherPerson)) mustBe true
    // ... but person2's own subtree must NOT bleed in - the walk stops at the foreign root.
    result.contains(result.getResource(otherPerson), result.getProperty(label), "Bob") mustBe false
    result.contains(result.getResource(otherPerson), result.getProperty(salary), "100") mustBe false
  }

  it should "not follow a shared object hub (e.g. a shared rdf:type class) backward when backward predicates are restricted" in {
    val pub1 = "http://example.org/publisher/1"
    val pub2 = "http://example.org/publisher/2"
    val orgType = "http://example.org/Organization"
    val typeProp = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type"

    val model = ModelFactory.createDefaultModel()
    stmt(model, pub1, typeProp, orgType)   // pub1 a Organization
    stmt(model, pub2, typeProp, orgType)   // pub2 a Organization - shares the class IRI

    // Restricting backward following to an empty set of inverse predicates: the shared class IRI must
    // not act as a hub that drags pub2 into pub1's subgraph.
    val restricted = ProjectUtils.connectedSubgraph(model, Seq(pub1), backwardPredicates = Some(Set.empty))
    restricted.contains(restricted.getResource(pub1), restricted.getProperty(typeProp), restricted.getResource(orgType)) mustBe true
    restricted.contains(restricted.getResource(pub2), restricted.getProperty(typeProp), restricted.getResource(orgType)) mustBe false

    // Legacy behaviour (no restriction) would bleed pub2 in via the shared class IRI.
    val unrestricted = ProjectUtils.connectedSubgraph(model, Seq(pub1))
    unrestricted.contains(unrestricted.getResource(pub2), unrestricted.getProperty(typeProp), unrestricted.getResource(orgType)) mustBe true
  }

  it should "still follow an incoming edge whose predicate is a declared backward mapping" in {
    val model = ModelFactory.createDefaultModel()
    literal(model, person, label, "Alice")
    stmt(model, address, addressOf, person)    // backward: address -> addressOf -> person
    literal(model, address, city, "Berlin")

    // addressOf is declared as an inverse-mapping predicate, so the linked child is still collected.
    val result = ProjectUtils.connectedSubgraph(model, Seq(person), backwardPredicates = Some(Set(addressOf)))
    result.contains(result.getResource(address), result.getProperty(addressOf), result.getResource(person)) mustBe true
    result.contains(result.getResource(address), result.getProperty(city), "Berlin") mustBe true
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
