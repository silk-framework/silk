package org.silkframework.plugins.dataset.rdf

import org.scalatest.mock.MockitoSugar
import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.config.{PlainTask, Task, TaskSpec}
import org.silkframework.dataset.Dataset
import org.silkframework.dataset.rdf.{SparqlEndpoint, SparqlParams}
import org.silkframework.entity._
import org.silkframework.execution.local.{LocalDatasetExecutor, LocalExecution, NestedEntityTable}
import org.silkframework.util.Uri

/**
  * Test read and write capabilities of generic implementation of SPARQL sink.
  */
class SparqlSinkTest extends FlatSpec with MustMatchers with MockitoSugar {
  behavior of "Sparql Sink"
  val SUBJ = "http://a"
  val PROP = "http://b"

  it should "generate valid statements based on the lexical value representation" in {
    val sink = new SparqlSink(SparqlParams(), mock[SparqlEndpoint])
    sink.buildStatementString(SUBJ, PROP, "test", AutoDetectValueType) must endWith(" \"test\" .\n")
    sink.buildStatementString(SUBJ, PROP, "123", AutoDetectValueType) must endWith(" \"123\"^^<http://www.w3.org/2001/XMLSchema#integer> .\n")
    sink.buildStatementString(SUBJ, PROP, "123.45", AutoDetectValueType) must endWith(" \"123.45\"^^<http://www.w3.org/2001/XMLSchema#double> .\n")
    sink.buildStatementString(SUBJ, PROP, "http://url.org", AutoDetectValueType) must endWith(" <http://url.org> .\n")
    sink.buildStatementString(SUBJ, PROP, "http://url.org Some Text", AutoDetectValueType) must endWith(" \"http://url.org Some Text\" .\n")
  }

  private val CITY_PROP = "http://City"
  private val POST_CODE_PROP = "http://PostCode"
  private val STREET_PROP = "http://Street"
  private val ID_PROP = "http://ID"
  private val AGE_PROP = "http://Age"
  private val NAME_PROP = "http://Name"
  private val PERSONAL_PROP = "http://personal"
  private val ADDRESS_PROP = "http://address"
  private val NESTED_PROP = "http://nestedLevel1"
  private val NESTED_PROP_2 = "http://nestedLevel2"

  val nestedSchema = NestedEntitySchema(
    NestedSchemaNode(
      entitySchema = entitySchema(ID_PROP),
      nestedEntities = IndexedSeq(
        nestedEntitySchema(
          entitySchema(NAME_PROP, AGE_PROP),
          connectionProperty = PERSONAL_PROP
        ),
        nestedEntitySchema(
          entitySchema(STREET_PROP, POST_CODE_PROP, CITY_PROP),
          connectionProperty = ADDRESS_PROP
        ),
        nestedEntitySchema(
          entitySchema(),
          connectionProperty = NESTED_PROP,
          IndexedSeq(
            nestedEntitySchema(
              entitySchema("http://incrediblyNestedValue"),
              connectionProperty = NESTED_PROP_2
            )
          )
        )
      )
    )
  )

  private val ROOT_RESOURCE = "http://entityRoot1"
  private val NESTED_RESOURCE_1 = "http://incrediblyNestedResource1"
  private val NESTED_RESOURCE_2 = "http://incrediblyNestedValueResource2"

  val nestedEntity = NestedEntity(
    ROOT_RESOURCE,
    values = IndexedSeq(Seq("1")),
    nestedEntities = IndexedSeq(
      Seq(
        NestedEntity(
          uri = PERSONAL_PROP + "Info1",
          IndexedSeq(Seq("Max Mustermann"), Seq("30")),
          nestedEntities = IndexedSeq())),
      Seq(
        NestedEntity(
          uri = ADDRESS_PROP + "Info1",
          IndexedSeq(Seq("Some alley 1"), Seq("12345"), Seq("Berlin")),
          nestedEntities = IndexedSeq()),
        NestedEntity(
          uri = ADDRESS_PROP + "Info2",
          IndexedSeq(Seq("Old street 23"), Seq("55522"), Seq("Hamburg")),
          nestedEntities = IndexedSeq())
      ),
      Seq(
        NestedEntity(
          uri = NESTED_RESOURCE_1,
          IndexedSeq(),
          nestedEntities = IndexedSeq(
            Seq(NestedEntity(
              uri = NESTED_RESOURCE_2,
              IndexedSeq(Seq("very nested value")),
              nestedEntities = IndexedSeq()
            ))
          )
        )
      )
    ))

  private val ADDRESS_RESOURCE1 = ADDRESS_PROP + "Info1"
  private val ADDRESS_RESOURCE2 = ADDRESS_PROP + "Info2"
  private val PERSONAL_RESOURCE = PERSONAL_PROP + "Info1"

  val expectedNestedTriples = List(
    (ADDRESS_RESOURCE1, CITY_PROP, "Berlin"),
    (ADDRESS_RESOURCE1, POST_CODE_PROP, "12345"),
    (ADDRESS_RESOURCE1, STREET_PROP, "Some alley 1"),
    (ADDRESS_RESOURCE2, CITY_PROP, "Hamburg"),
    (ADDRESS_RESOURCE2, POST_CODE_PROP, "55522"),
    (ADDRESS_RESOURCE2, STREET_PROP, "Old street 23"),
    (ROOT_RESOURCE, ID_PROP, "1"),
    (ROOT_RESOURCE, ADDRESS_PROP, ADDRESS_RESOURCE1),
    (ROOT_RESOURCE, ADDRESS_PROP, ADDRESS_RESOURCE2),
    (ROOT_RESOURCE, NESTED_PROP, NESTED_RESOURCE_1),
    (ROOT_RESOURCE, PERSONAL_PROP, PERSONAL_RESOURCE),
    (NESTED_RESOURCE_1, NESTED_PROP_2, NESTED_RESOURCE_2),
    (NESTED_RESOURCE_2, "http://incrediblyNestedValue", "very nested value"),
    (PERSONAL_RESOURCE, AGE_PROP, "30"),
    (PERSONAL_RESOURCE, NAME_PROP, "Max Mustermann")
  )

  it should "generate correct triples when writing nested entities" in {
    val inMemoryDataset = InMemoryDataset()
    val localDatasetExecutor = new LocalDatasetExecutor()
    val datasetTask: Task[Dataset] = PlainTask("output", inMemoryDataset)
    val mockTask = PlainTask("input", mock[TaskSpec])
    val input = NestedEntityTable(Seq(nestedEntity), nestedSchema, mockTask)
    val endpoint = inMemoryDataset.sparqlEndpoint
    endpoint.select("""SELECT * WHERE {?s ?p ?o}""").bindings.size mustBe 0
    localDatasetExecutor.execute(datasetTask, inputs = Seq(input), outputSchema = None, execution = LocalExecution(true))
    val triples = endpoint.select("""SELECT * WHERE {?s ?p ?o}""").bindings.toSeq
    triples.size must be > 0
    val triplesValues = triples map (t => (t("s").value, t("p").value, t("o").value))
    val triplesSorted = triplesValues.sortWith { case ((s, p, o), (s2, p2, o2)) => s < s2 || s == s2 && p < p2 || s == s2 && p == p2 && o < o2 }
    triplesSorted mustBe expectedNestedTriples
  }

  private def entitySchema(paths: String*): EntitySchema = {
    val typedPaths = paths map (pathStr => Path(pathStr).asStringTypedPath)
    EntitySchema(typeUri = Uri(""), typedPaths = typedPaths.toIndexedSeq)
  }

  private def nestedEntitySchema(entitySchema: EntitySchema,
                                 connectionProperty: String,
                                 nestedEntitySchemas: IndexedSeq[(EntitySchemaConnection, NestedSchemaNode)] = IndexedSeq.empty
                                ): (EntitySchemaConnection, NestedSchemaNode) = {
    (entitySchemaConnection(connectionProperty),
        NestedSchemaNode(
          entitySchema,
          nestedEntitySchemas
        )
    )
  }

  private def entitySchemaConnection(connectionProperty: String): EntitySchemaConnection = {
    EntitySchemaConnection(Path(connectionProperty))
  }
}
