package org.silkframework.plugins.dataset.rdf.sparql

import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.config.Prefixes
import org.silkframework.dataset.rdf.SparqlEndpoint
import org.silkframework.entity.{EntitySchema, Path, Restriction, TypedPath}
import org.silkframework.plugins.dataset.rdf.RdfFileDataset
import org.silkframework.runtime.resource.{ClasspathResourceLoader, ReadOnlyResource}
import org.silkframework.util.Uri

abstract class EntityRetrieverBaseTest extends FlatSpec with MustMatchers {
  def entityRetriever(endpoint: SparqlEndpoint,
                      graphUri: Option[String] = None,
                      useOrderBy: Boolean = true): EntityRetriever

  lazy val endpoint: SparqlEndpoint = {
    val resource = ReadOnlyResource(ClasspathResourceLoader("org/silkframework/plugins/dataset/rdf").get("persons.ttl"))
    RdfFileDataset(resource, "Turtle").sparqlEndpoint
  }

  private val pn: String = "https://ns.eccenca.com/source/"

  implicit private val prefixes: Prefixes = Prefixes(Map(
    "pn" -> pn
  ))

  private val Person = s"${pn}Person"
  private val Address = s"${pn}Address"
  private val address = s"${pn}address"
  private val addressInBerlin = Path.parse(s"""pn:address[pn:city="Berlin"]""")
  private val addressBackwards = Path.parse("\\pn:address")
  private val city = s"${pn}city"
  private val country = s"${pn}country"
  private val name = s"${pn}name"

  // Instances
  private val person1 = s"${pn}Person1"
  private val person2 = s"${pn}Person2"
  private val address1 = s"${pn}Address1"
  private val address2 = s"${pn}Address2"
  private val address3 = s"${pn}Address3"

  private def retriever = entityRetriever(endpoint)

  private def path(propertyUri: String): TypedPath = Path.parse(s"<$propertyUri>").asStringTypedPath
  private def path(properties: Seq[String]): TypedPath = Path.parse(properties.mkString("/<", ">/<", ">")).asStringTypedPath

  it should "fetch root entities" in {
    val entitySchema = schema(Person, Seq(path(name)))
    val entities = retriever.retrieve(entitySchema, entities = Seq(), limit = None).toArray.toSeq
    entities.size mustBe 2
    entities.map(_.uri.toString) mustBe Seq(person1, person2)
  }

  it should "fetch multi-hop paths" in {
    val entitySchema = schema(Person, Seq(path(Seq(address, city)), path(Seq(address, country))))
    val entities = retriever.retrieve(entitySchema, entities = Seq(), limit = None).toArray.toSeq
    entities.size mustBe 2
    entities.map(_.uri.toString) mustBe Seq(person1, person2)
    entities.head.values mustBe IndexedSeq(Seq("Berlin", "Stuttgart"), Seq("Germany"))
    entities(1).values mustBe IndexedSeq(Seq("Leipzig"), Seq("Germany"))
  }

  it should "fetch restricted root entities" in {
    val entitySchema = schema(Person, Seq(path(Seq(address, city)), path(Seq(address, country))),
      filter = Restriction.custom(s"?a <${pn}age> 23"))
    val entities = retriever.retrieve(entitySchema, entities = Seq(), limit = None).toArray.toSeq
    entities.map(_.uri.toString) mustBe Seq(person1)
    entities.head.values mustBe IndexedSeq(Seq("Berlin", "Stuttgart"), Seq("Germany"))
  }

  it should "understand sub paths" in {
    val entitySchema = schema(Person, Seq(path(city), path(country)),
      subPath = path(address))
    val entities = retriever.retrieve(entitySchema, entities = Seq(), limit = None).toArray.toSeq
    entities.map(_.uri.toString) mustBe Seq(address1, address2, s"${pn}Address3")
    entities.head.values mustBe IndexedSeq(Seq("Berlin"), Seq("Germany"))
    entities(2).values mustBe IndexedSeq(Seq("Leipzig"), Seq("Germany"))
  }

  it should "understand sub paths with filters" in {
    val entitySchema = schema(Person, Seq(path(city), path(country)), subPath = addressInBerlin)
    val entities = retriever.retrieve(entitySchema, entities = Seq(), limit = None).toArray.toSeq
    entities.map(_.uri.toString) mustBe Seq(address1)
    entities.head.values mustBe IndexedSeq(Seq("Berlin"), Seq("Germany"))
  }

  it should "understand sub paths with backward operators" in {
    val entitySchema = schema(Address, Seq(path(name)), subPath = addressBackwards)
    val entities = retriever.retrieve(entitySchema, entities = Seq(), limit = None).toArray.toSeq
    entities.map(_.uri.toString) mustBe Seq(person1, person2)
    entities.head.values mustBe IndexedSeq(Seq("John Doe"))
    entities(1).values mustBe IndexedSeq(Seq("Max Power"))
  }

  it should "process sub paths with restricted root entities" in {
    val entitySchema = schema(Person, Seq(path(city), path(country)),
      subPath = path(address), filter = Restriction.custom(s"?a <${pn}age> 23"))
    val entities = retriever.retrieve(entitySchema, entities = Seq(), limit = None).toArray.toSeq
    entities.map(_.uri.toString) mustBe Seq(address1, address2)
    entities.head.values mustBe IndexedSeq(Seq("Berlin"), Seq("Germany"))
    entities(1).values mustBe IndexedSeq(Seq("Stuttgart"), Seq("Germany"))
  }

  it should "fetch a specific root entity" in {
    val entitySchema = schema(Person, Seq(path(name)))
    val entities = retriever.retrieve(entitySchema, entities = Seq(person1), limit = None).toArray.toSeq
    entities.map(_.uri.toString) mustBe Seq(person1)
    entities.head.values mustBe IndexedSeq(Seq("John Doe"))
  }

  it should "fetch a specific root entity with restrictions" in {
    for((personURI, expectedResult) <- Seq(person1, person2).zip(Seq(Seq("John Doe"), Seq()))) {
      val entitySchema = schema(Person, Seq(path(name)), filter = Restriction.custom(s"?a <${pn}age> 23"))
      val entities = retriever.retrieve(entitySchema, entities = Seq(personURI), limit = None).toArray.toSeq
      entities.size mustBe expectedResult.size
      if(entities.nonEmpty) {
        entities.head.uri.toString mustBe personURI
        entities.head.values mustBe IndexedSeq(expectedResult)
      }
    }
  }

  it should "restrict the root entity URI and work with sub path" in {
    val entitySchema = schema(Person, Seq(path(city), path(country)),
      subPath = path(address))
    val entities = retriever.retrieve(entitySchema, entities = Seq(person1), limit = None).toArray.toSeq
    entities.map(_.uri.toString) mustBe Seq(address1, address2)
    entities.head.values mustBe IndexedSeq(Seq("Berlin"), Seq("Germany"))
    entities(1).values mustBe IndexedSeq(Seq("Stuttgart"), Seq("Germany"))
  }

  it should "restrict the root entity URI and work with sub path and restriction" in {
    for((personURI, (addressURI, expectedResult)) <- Seq(person1, person2).zip(Seq(address1, address3).zip(Seq(IndexedSeq(), IndexedSeq(Seq("Leipzig"), Seq("Germany")))))) {
      val entitySchema = schema(Person, Seq(path(city), path(country)), filter = Restriction.custom(s"?a <${pn}age> 55"),
        subPath = path(address))
      val entities = retriever.retrieve(entitySchema, entities = Seq(personURI), limit = None).toArray.toSeq
      entities.size mustBe math.min(expectedResult.size, 1)
      if(entities.nonEmpty) {
        entities.head.uri.toString mustBe addressURI
        entities.head.values mustBe expectedResult
      }
    }
  }

  private def schema(typeUri: String,
                     typedPaths: Seq[TypedPath],
                     filter: Restriction = Restriction.empty,
                     subPath: Path = Path.empty) = {
    EntitySchema(Uri(typeUri), typedPaths = typedPaths.toIndexedSeq, filter, subPath)
  }
}
