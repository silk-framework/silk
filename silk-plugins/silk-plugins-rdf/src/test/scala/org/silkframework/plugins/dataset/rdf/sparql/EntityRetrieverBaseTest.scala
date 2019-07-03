package org.silkframework.plugins.dataset.rdf.sparql

import org.apache.jena.query.DatasetFactory
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.{Lang, RDFDataMgr}
import org.scalatest.{BeforeAndAfterAll, FlatSpec, MustMatchers}
import org.silkframework.config.Prefixes
import org.silkframework.dataset.rdf.SparqlEndpoint
import org.silkframework.entity.paths.{TypedPath, UntypedPath}
import org.silkframework.entity.{EntitySchema, Restriction}
import org.silkframework.plugins.dataset.rdf.datasets.SparqlDataset
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource.ClasspathResourceLoader
import org.silkframework.util.Uri

abstract class EntityRetrieverBaseTest extends FlatSpec with MustMatchers with BeforeAndAfterAll {
  def entityRetriever(endpoint: SparqlEndpoint,
                      graphUri: Option[String] = None,
                      useOrderBy: Boolean = true): EntityRetriever
  private val GRAPH = "http://testGraph"
  private var fusekiServerInfo: Option[FusekiServerInfo] = None

  implicit val userContext: UserContext = UserContext.Empty

  lazy val endpoint: SparqlEndpoint = {
    val fusekiUrl = fusekiServerInfo.getOrElse(throw new RuntimeException("Did not start Fuseki server!")).url
    SparqlDataset(endpointURI = fusekiUrl, graph = GRAPH).sparqlEndpoint
  }

  override def beforeAll(): Unit = {
    val dataset = DatasetFactory.createTxnMem
    val model = ModelFactory.createDefaultModel()
    val is = ClasspathResourceLoader("org/silkframework/plugins/dataset/rdf").get("persons.ttl").inputStream
    RDFDataMgr.read(model, is, Lang.TURTLE)
    dataset.addNamedModel(GRAPH, model)
    fusekiServerInfo = Some(FusekiHelper.startFusekiServer(dataset, startPort = 3330))
  }

  override def afterAll(): Unit = {
    fusekiServerInfo foreach (s => s.server.stop())
  }

  private val pn: String = "https://ns.eccenca.com/source/"

  implicit private val prefixes: Prefixes = Prefixes(Map(
    "pn" -> pn
  ))

  private val Person = s"${pn}Person"
  private val Address = s"${pn}Address"
  private val address = s"${pn}address"
  private val addressInBerlin = UntypedPath.parse(s"""pn:address[pn:city="Berlin"]""")
  private val addressBackwards = UntypedPath.parse("\\pn:address")
  private val city = s"${pn}city"
  private val country = s"${pn}country"
  private val name = s"${pn}name"

  // Instances
  private val person1 = s"${pn}Person1"
  private val person2 = s"${pn}Person2"
  private val address1 = s"${pn}Address1"
  private val address2 = s"${pn}Address2"
  private val address3 = s"${pn}Address3"

  private def retriever = entityRetriever(endpoint, graphUri = Some(GRAPH))

  private def path(propertyUri: String): TypedPath = UntypedPath.parse(s"<$propertyUri>").asStringTypedPath
  private def path(properties: Seq[String]): TypedPath = UntypedPath.parse(properties.mkString("/<", ">/<", ">")).asStringTypedPath

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
      subPath = path(address).toSimplePath)
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
      subPath = path(address).toSimplePath, filter = Restriction.custom(s"?a <${pn}age> 23"))
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
      subPath = path(address).toSimplePath)
    val entities = retriever.retrieve(entitySchema, entities = Seq(person1), limit = None).toArray.toSeq
    entities.map(_.uri.toString) mustBe Seq(address1, address2)
    entities.head.values mustBe IndexedSeq(Seq("Berlin"), Seq("Germany"))
    entities(1).values mustBe IndexedSeq(Seq("Stuttgart"), Seq("Germany"))
  }

  it should "restrict the root entity URI and work with sub path and restriction" in {
    for((personURI, (addressURI, expectedResult)) <- Seq(person1, person2).zip(Seq(address1, address3).zip(Seq(IndexedSeq(), IndexedSeq(Seq("Leipzig"), Seq("Germany")))))) {
      val entitySchema = schema(Person, Seq(path(city), path(country)), filter = Restriction.custom(s"?a <${pn}age> 55"),
        subPath = path(address).toSimplePath)
      val entities = retriever.retrieve(entitySchema, entities = Seq(personURI), limit = None).toArray.toSeq
      entities.size mustBe math.min(expectedResult.size, 1)
      if(entities.nonEmpty) {
        entities.head.uri.toString mustBe addressURI
        entities.head.values mustBe expectedResult
      }
    }
  }

  it should "respect the configured query limit" in {
    val entitySchema = schema(Person, Seq(path(name)))
    val entities = retriever.retrieve(entitySchema, entities = Seq(), limit = Some(1)).toArray.toSeq
    entities.size mustBe 1
  }

  private def schema(typeUri: String,
                     typedPaths: Seq[TypedPath],
                     filter: Restriction = Restriction.empty,
                     subPath: UntypedPath = UntypedPath.empty) = {
    EntitySchema(Uri(typeUri), typedPaths = typedPaths.toIndexedSeq, filter, subPath)
  }
}
