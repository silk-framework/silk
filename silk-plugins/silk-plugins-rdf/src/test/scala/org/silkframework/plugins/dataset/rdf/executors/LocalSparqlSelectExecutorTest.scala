package org.silkframework.plugins.dataset.rdf.executors


import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.silkframework.config.{PlainTask, Prefixes, Task}
import org.silkframework.dataset.rdf._
import org.silkframework.dataset.{DataSource, DatasetSpec, EntitySink, LinkSink}
import org.silkframework.entity.paths.TypedPath
import org.silkframework.entity.{Entity, EntitySchema, ValueType}
import org.silkframework.execution.ReportingIterator
import org.silkframework.execution.local.GenericEntityTable
import org.silkframework.plugins.dataset.rdf.tasks.SparqlSelectCustomTask
import org.silkframework.runtime.activity.{TestUserContextTrait, UserContext}
import org.silkframework.runtime.iterator.{CloseableIterator, TraversableIterator}
import org.silkframework.runtime.plugin.{ParameterValues, PluginContext}
import org.silkframework.runtime.templating.exceptions.UnboundVariablesException
import org.silkframework.util.{MockitoSugar, TestMocks}

import scala.collection.immutable.SortedMap

class LocalSparqlSelectExecutorTest extends AnyFlatSpec
    with Matchers
    with TestUserContextTrait
    with MockitoSugar {
  behavior of "Local SPARQL Select executor"

  val timeout = 50
  implicit val pluginContext: PluginContext = PluginContext.empty

  it should "not run out of memory and fetch first entity immediately on large result sets" in {
    val quickReactionTime = 500 // quick in the sense that it won't take too long even on a heavy-loaded CI system
    val activityContextMock = TestMocks.activityContextMock()
    val task = SparqlSelectCustomTask("SELECT * WHERE {?s ?p ?o}")
    val reportUpdater = SparqlSelectExecutionReportUpdater(PlainTask("task", task), activityContextMock)
    val sparqlEndpoint = new SparqlEndpoint {
      override def sparqlParams: SparqlParams = SparqlParams()
      override def withSparqlParams(sparqlParams: SparqlParams): SparqlEndpoint = this
      override def select(query: String, limit: Int)(implicit userContext: UserContext): SparqlResults = {
        val entities =
          for(i <- Iterator.range(0, limit)) yield {
            SortedMap("s" -> Resource(s"subject $i"), "p" -> Resource(s"predicate $i"), "o" -> PlainLiteral(s"literal $i"))
          }
        SparqlResults(Seq("s", "p", "o"), CloseableIterator(entities))
      }
      override def ask(query: String)(implicit userContext: UserContext): SparqlAskResult = ???
    }
    Entity.empty("") // Make sure that Entity class is loaded
    val start = System.currentTimeMillis()
    val entities = LocalSparqlSelectExecutor().executeOnSparqlEndpoint(task, taskWithEndpoint(sparqlEndpoint), None, executionReportUpdater = Some(reportUpdater))
    val entity = entities.head
    entity.values.flatten.head mustBe "subject 0"
    (System.currentTimeMillis() - start).toInt must be < quickReactionTime
  }


  it should "pass query timeout to SPARQL endpoint if a query timeout is configured" in {
    val task = SparqlSelectCustomTask("SELECT * WHERE {?s ?p ?o}", sparqlTimeout = timeout)
    var correctTimeout = false
    val activityContextMock = TestMocks.activityContextMock()
    val reportUpdater = SparqlSelectExecutionReportUpdater(PlainTask("task", task), activityContextMock)
    val sparqlEndpoint = sparqlEndpointStub(selectCallback = endpoint => {
      correctTimeout = endpoint.sparqlParams.timeout.contains(timeout)
    })
    val limit = 1000 * 1000 * 1000
    val entities = LocalSparqlSelectExecutor().executeOnSparqlEndpoint(task, taskWithEndpoint(sparqlEndpoint), None, limit, Some(reportUpdater))
    entities.headOption // Needed to actually execute the query
    correctTimeout mustBe true
  }

  it should "generate one query per input entity when useDefaultDataset is set and the template references entity values" in {
    val query = """SELECT ?p ?o WHERE { <{{ input.entity.s }}> ?p ?o }"""
    val rowsPerQuery = 2
    val task = SparqlSelectCustomTask(query, limit = rowsPerQuery.toString, useDefaultDataset = true)

    val capturedQueries = collection.mutable.ArrayBuffer.empty[String]
    val sparqlEndpoint = sparqlEndpointStub(queryCapture = q => capturedQueries += q)
    val stubDataset = new StubRdfDataset(sparqlEndpoint)

    val inputSchema = EntitySchema("", typedPaths = IndexedSeq(TypedPath("s", ValueType.URI)))
    val inputEntities = Seq(
      Entity("urn:in:1", IndexedSeq(Seq("http://example.org/a")), inputSchema),
      Entity("urn:in:2", IndexedSeq(Seq("http://example.org/b")), inputSchema)
    )
    val inputTable = GenericEntityTable(inputEntities, inputSchema, PlainTask("inputTask", DatasetSpec(stubDataset)))

    val activityContextMock = TestMocks.activityContextMock()
    val reportUpdater = SparqlSelectExecutionReportUpdater(PlainTask("task", task), activityContextMock)

    val results = LocalSparqlSelectExecutor()
      .executeOnDefaultDatasetPerEntity(task, stubDataset, inputTable, outputTask = None, executionReportUpdater = reportUpdater)
      ._1
      .toList

    capturedQueries.toSeq must have size 2
    capturedQueries(0) must include ("<http://example.org/a>")
    capturedQueries(1) must include ("<http://example.org/b>")
    // Bindings from both queries are flattened into the output: rowsPerQuery rows × 2 queries.
    results.size mustBe (rowsPerQuery * 2)
  }

  it should "fail when an input entity is missing a value referenced by the template" in {
    val query = """SELECT ?p ?o WHERE { <{{ input.entity.s }}> ?p ?o }"""
    val task = SparqlSelectCustomTask(query, useDefaultDataset = true)

    val sparqlEndpoint = sparqlEndpointStub()
    val stubDataset = new StubRdfDataset(sparqlEndpoint)

    val inputSchema = EntitySchema("", typedPaths = IndexedSeq(TypedPath("s", ValueType.URI)))
    val inputEntities = Seq(
      Entity("urn:in:1", IndexedSeq(Seq()), inputSchema)
    )
    val inputTable = GenericEntityTable(inputEntities, inputSchema, PlainTask("inputTask", DatasetSpec(stubDataset)))

    val activityContextMock = TestMocks.activityContextMock()
    val reportUpdater = SparqlSelectExecutionReportUpdater(PlainTask("task", task), activityContextMock)

    an[UnboundVariablesException] must be thrownBy {
      LocalSparqlSelectExecutor()
        .executeOnDefaultDatasetPerEntity(task, stubDataset, inputTable, outputTask = None, executionReportUpdater = reportUpdater)
        ._1
        .toList
    }
  }

  it should "evaluate a Jinja query template using the graph variable from the task parameters" in {
    val graphUri = "http://example.org/testGraph"
    val query = """SELECT * WHERE { GRAPH <{{ input.config.graph }}> { ?s ?p ?o } }"""
    val task = SparqlSelectCustomTask(query)
    var capturedQuery = ""
    val activityContextMock = TestMocks.activityContextMock()
    val reportUpdater = SparqlSelectExecutionReportUpdater(PlainTask("task", task), activityContextMock)
    val sparqlEndpoint = sparqlEndpointStub(queryCapture = q => capturedQuery = q)
    LocalSparqlSelectExecutor().executeOnSparqlEndpoint(task, taskWithEndpoint(sparqlEndpoint, graphUri = Some(graphUri)), None, executionReportUpdater = Some(reportUpdater)).headOption

    task.outputSchema.typedPaths.map(_.toUntypedPath.normalizedSerialization) mustBe IndexedSeq("s", "p", "o")
    capturedQuery must include(s"<$graphUri>")
    capturedQuery must not include "input.config.graph"
  }

  it should "derive the output schema from the runtime result variables when it cannot be inferred from the template" in {
    // The projection is produced by a placeholder, so the variables cannot be extracted statically.
    val task = SparqlSelectCustomTask("""SELECT {{ input.config.graph }} WHERE { ?s ?p ?o }""")
    task.outputSchema.typedPaths mustBe empty // precondition: schema is unknown statically -> UnknownSchemaPort

    val reportUpdater = SparqlSelectExecutionReportUpdater(PlainTask("task", task), TestMocks.activityContextMock())
    val sparqlEndpoint = sparqlEndpointStub()
    val entities = LocalSparqlSelectExecutor()
      .executeOnSparqlEndpoint(task, taskWithEndpoint(sparqlEndpoint, graphUri = Some("http://example.org/g")), None,
        limit = 2, executionReportUpdater = Some(reportUpdater))

    entities.effectiveSchema.typedPaths.map(_.toUntypedPath.normalizedSerialization) mustBe IndexedSeq("s", "p", "o")
    val first = entities.head
    first.schema.typedPaths.map(_.toUntypedPath.normalizedSerialization) mustBe IndexedSeq("s", "p", "o")
    first.values mustBe IndexedSeq(Seq("subject 0"), Seq("predicate 0"), Seq("literal 0"))
  }

  it should "fetch the first entity immediately on large result sets when the schema is derived at runtime" in {
    val quickReactionTime = 500
    val task = SparqlSelectCustomTask("""SELECT {{ input.config.graph }} WHERE { ?s ?p ?o }""")
    task.outputSchema.typedPaths mustBe empty
    val reportUpdater = SparqlSelectExecutionReportUpdater(PlainTask("task", task), TestMocks.activityContextMock())
    val sparqlEndpoint = new SparqlEndpoint {
      override def sparqlParams: SparqlParams = SparqlParams()
      override def withSparqlParams(sparqlParams: SparqlParams): SparqlEndpoint = this
      override def select(query: String, limit: Int)(implicit userContext: UserContext): SparqlResults = {
        val entities =
          for (i <- Iterator.range(0, limit)) yield {
            SortedMap[String, RdfNode]("s" -> Resource(s"subject $i"), "p" -> Resource(s"predicate $i"), "o" -> PlainLiteral(s"literal $i"))
          }
        SparqlResults(Seq("s", "p", "o"), CloseableIterator(entities))
      }
      override def ask(query: String)(implicit userContext: UserContext): SparqlAskResult = ???
    }
    Entity.empty("") // Make sure that Entity class is loaded
    val start = System.currentTimeMillis()
    val entities = LocalSparqlSelectExecutor()
      .executeOnSparqlEndpoint(task, taskWithEndpoint(sparqlEndpoint, graphUri = Some("http://example.org/g")), None, executionReportUpdater = Some(reportUpdater))
    entities.effectiveSchema.typedPaths.map(_.toUntypedPath.normalizedSerialization) mustBe IndexedSeq("s", "p", "o")
    val entity = entities.head
    entity.values.flatten.head mustBe "subject 0"
    (System.currentTimeMillis() - start).toInt must be < quickReactionTime
  }

  it should "derive the schema from the result variables even when the result set is empty" in {
    val task = SparqlSelectCustomTask("""SELECT {{ input.config.graph }} WHERE { ?s ?p ?o }""")
    task.outputSchema.typedPaths mustBe empty
    val reportUpdater = SparqlSelectExecutionReportUpdater(PlainTask("task", task), TestMocks.activityContextMock())
    val sparqlEndpoint = new SparqlEndpoint {
      override def sparqlParams: SparqlParams = SparqlParams()
      override def withSparqlParams(sparqlParams: SparqlParams): SparqlEndpoint = this
      override def select(query: String, limit: Int)(implicit userContext: UserContext): SparqlResults =
        SparqlResults(Seq("s", "p", "o"), CloseableIterator.empty)
      override def ask(query: String)(implicit userContext: UserContext): SparqlAskResult = ???
    }
    val entities = LocalSparqlSelectExecutor()
      .executeOnSparqlEndpoint(task, taskWithEndpoint(sparqlEndpoint, graphUri = Some("http://example.org/g")), None, executionReportUpdater = Some(reportUpdater))
    entities.effectiveSchema.typedPaths.map(_.toUntypedPath.normalizedSerialization) mustBe IndexedSeq("s", "p", "o")
    entities.toList mustBe empty
  }

  it should "use the statically inferred schema without querying the endpoint" in {
    val task = SparqlSelectCustomTask("SELECT ?a ?b WHERE { ?a ?p ?b }")
    task.outputSchema.typedPaths.map(_.toUntypedPath.normalizedSerialization) mustBe IndexedSeq("a", "b")
    val reportUpdater = SparqlSelectExecutionReportUpdater(PlainTask("task", task), TestMocks.activityContextMock())
    val sparqlEndpoint = new SparqlEndpoint {
      override def sparqlParams: SparqlParams = SparqlParams()
      override def withSparqlParams(sparqlParams: SparqlParams): SparqlEndpoint = this
      override def select(query: String, limit: Int)(implicit userContext: UserContext): SparqlResults =
        throw new RuntimeException("The endpoint must not be queried to compute a statically known schema.")
      override def ask(query: String)(implicit userContext: UserContext): SparqlAskResult = ???
    }
    val entities = LocalSparqlSelectExecutor()
      .executeOnSparqlEndpoint(task, taskWithEndpoint(sparqlEndpoint), None, executionReportUpdater = Some(reportUpdater))
    // Fast path: effectiveSchema returns the static schema without invoking select (which would throw).
    entities.effectiveSchema.typedPaths.map(_.toUntypedPath.normalizedSerialization) mustBe IndexedSeq("a", "b")
  }

  it should "derive the per-entity output schema from the runtime result variables when it cannot be inferred" in {
    val query = """SELECT {{ input.entity.s }} WHERE { <{{ input.entity.s }}> ?p ?o }"""
    val rowsPerQuery = 2
    val task = SparqlSelectCustomTask(query, limit = rowsPerQuery.toString, useDefaultDataset = true)
    task.outputSchema.typedPaths mustBe empty

    val sparqlEndpoint = sparqlEndpointStub()
    val stubDataset = new StubRdfDataset(sparqlEndpoint)
    val inputSchema = EntitySchema("", typedPaths = IndexedSeq(TypedPath("s", ValueType.URI)))
    val inputEntities = Seq(
      Entity("urn:in:1", IndexedSeq(Seq("http://example.org/a")), inputSchema),
      Entity("urn:in:2", IndexedSeq(Seq("http://example.org/b")), inputSchema)
    )
    val inputTable = GenericEntityTable(inputEntities, inputSchema, PlainTask("inputTask", DatasetSpec(stubDataset)))
    val reportUpdater = SparqlSelectExecutionReportUpdater(PlainTask("task", task), TestMocks.activityContextMock())

    val (entities, schema) = LocalSparqlSelectExecutor()
      .executeOnDefaultDatasetPerEntity(task, stubDataset, inputTable, outputTask = None, executionReportUpdater = reportUpdater)

    schema.typedPaths.map(_.toUntypedPath.normalizedSerialization) mustBe IndexedSeq("s", "p", "o")
    val rows = entities.toList
    rows.size mustBe (rowsPerQuery * 2)
    rows.foreach(_.schema.typedPaths.map(_.toUntypedPath.normalizedSerialization) mustBe IndexedSeq("s", "p", "o"))
  }

  it should "produce empty per-entity output without querying the endpoint when there are no input entities" in {
    val query = """SELECT {{ input.entity.s }} WHERE { <{{ input.entity.s }}> ?p ?o }"""
    val task = SparqlSelectCustomTask(query, useDefaultDataset = true)
    task.outputSchema.typedPaths mustBe empty

    val capturedQueries = collection.mutable.ArrayBuffer.empty[String]
    val sparqlEndpoint = sparqlEndpointStub(queryCapture = q => capturedQueries += q)
    val stubDataset = new StubRdfDataset(sparqlEndpoint)
    val inputSchema = EntitySchema("", typedPaths = IndexedSeq(TypedPath("s", ValueType.URI)))
    val inputTable = GenericEntityTable(Seq.empty[Entity], inputSchema, PlainTask("inputTask", DatasetSpec(stubDataset)))
    val reportUpdater = SparqlSelectExecutionReportUpdater(PlainTask("task", task), TestMocks.activityContextMock())

    val (entities, schema) = LocalSparqlSelectExecutor()
      .executeOnDefaultDatasetPerEntity(task, stubDataset, inputTable, outputTask = None, executionReportUpdater = reportUpdater)

    schema.typedPaths mustBe empty
    entities.toList mustBe empty
    capturedQueries mustBe empty
  }

  private def taskWithEndpoint(sparqlEndpoint: SparqlEndpoint, graphUri: Option[String] = None): Task[DatasetSpec[RdfDataset]] = {
    PlainTask("testDataset", DatasetSpec(new StubRdfDataset(sparqlEndpoint, graphUri)))
  }

  private class StubRdfDataset(endpoint: SparqlEndpoint, graphUri: Option[String] = None) extends RdfDataset {
    override def sparqlEndpoint: SparqlEndpoint = endpoint
    override def parameters(implicit pluginContext: PluginContext): ParameterValues = {
      graphUri match {
        case Some(g) => ParameterValues.fromStringMap(Map("graph" -> g))
        case None => ParameterValues.empty
      }
    }
    override def source(implicit userContext: UserContext): DataSource = ???
    override def linkSink(implicit userContext: UserContext): LinkSink = ???
    override def entitySink(implicit userContext: UserContext): EntitySink = ???
  }

  private def sparqlEndpointStub(selectCallback: SparqlEndpoint => Unit = _ => {},
                                 graphUri: Option[String] = None,
                                 queryCapture: String => Unit = _ => {}): SparqlEndpoint = {
    new SparqlEndpoint {
      var sparqlParamsIntern = SparqlParams(graph = graphUri)
      override def sparqlParams: SparqlParams = sparqlParamsIntern

      override def withSparqlParams(sparqlParams: SparqlParams): SparqlEndpoint = {
        sparqlParamsIntern = sparqlParams // This is not immutable, but is OK for the test
        this
      }

      override def select(query: String, limit: Int)(implicit userContext: UserContext): SparqlResults = {
        selectCallback(this)
        queryCapture(query)
        SparqlResults(Seq("s", "p", "o"), new TraversableIterator[SortedMap[String, RdfNode]] {
          override def foreach[U](f: SortedMap[String, RdfNode] => U): Unit = {
            var i = 0
            while (i < limit) {
              f(SortedMap("s" -> Resource(s"subject $i"), "p" -> Resource(s"predicate $i"), "o" -> PlainLiteral(s"literal $i")))
              i += 1
            }
          }
        })
      }
    }
  }
}