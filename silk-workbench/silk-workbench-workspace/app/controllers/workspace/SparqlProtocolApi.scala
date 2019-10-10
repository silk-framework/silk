package controllers.workspace

import controllers.core.RequestUserContextAction
import controllers.workspace.SparqlProtocolApi.SparqlContentNegotiationError
import javax.inject.Inject
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.dataset.rdf.RdfDataset
import org.silkframework.runtime.serialization.WriteContext
import org.silkframework.runtime.validation.{BadUserInputException, RequestException}
import org.silkframework.workbench.Context
import org.silkframework.workbench.utils.ErrorResult
import play.api.http.{MimeTypes, Status}
import play.api.libs.json.JsValue
import play.api.mvc.{Action, AnyContent, InjectedController}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
  * This API implements a SPARQL 1.1 protocol conform SPARQL endpoint for each [[RdfDataset]]
  * Thereby instantaneous SPARQL access via REST and SPARQL SERVICE keyword
  * to every [[RdfDataset]] is available without pushing triples to an active triple store.
  * FIXME CONSTRUCT and DESCRIBE queries are currently not supported, but should be easy to implement
  */
class SparqlProtocolApi @Inject() () extends InjectedController {

  private def getJsonWriteContext(context: Context[GenericDatasetSpec]): WriteContext[JsValue] = WriteContext[JsValue](
    None,
    context.project.config.prefixes,
    Some(context.project.config.id),
    context.project.config.projectResourceUriOpt
  )

  private def checkGraphParams(query: String, defaultGraphUri: List[String], namedGraphUri: List[String]): String ={
    if (defaultGraphUri.nonEmpty || namedGraphUri.nonEmpty) {
      //FIXME remove if named graphs are supported
      ErrorResult(METHOD_NOT_ALLOWED, "Graph specification currently not supported", "Specifying graphs using SPRRQL protocol parameters is not supported. The default graph contains all tripled of the dataset.")
    }
    query //FIXME should unify graphs named in a query (FROM...) and those specified via the protocol into a consistent query
  }

  private val askRegex = "(ask|ASK)\\s+(WHERE|where)".r
  def select(project: String, task: String, query: String = "", defaultGraphUri: List[String] = List(), namedGraphUri: List[String] = List()): Action[AnyContent] =
    askRegex.findFirstMatchIn(query) match{
      case Some(_) => ask(project, task, query, defaultGraphUri, namedGraphUri)
      case None => RequestUserContextAction {
        implicit request =>
        implicit userContext =>
        val updatedQuery = checkGraphParams(query, defaultGraphUri, namedGraphUri)
        val context = Context.get[GenericDatasetSpec](project, task, request.path)
        implicit val jsonWriteContext: WriteContext[JsValue] = getJsonWriteContext(context)

        context.task.data.plugin match {
          case rdf: RdfDataset =>
            val sparqlEndpoint = rdf.sparqlEndpoint
            val queryResults = sparqlEndpoint.select(updatedQuery)
            Ok(SparqlResultSerializer.write(queryResults))
          case _ =>
            ErrorResult(BadUserInputException("This is not an RDF-Dataset."))
        }
    }
  }

  def postSelect(project: String, task: String, defaultGraphUri: List[String] = List(), namedGraphUri: List[String] = List()): Action[AnyContent] = {
    RequestUserContextAction {
      implicit request =>
        implicit userContext =>
          request.contentType match{
            case Some(SparqlProtocolApi.SPARQL) =>
              val query = request.body.asRaw.flatMap(_.asBytes()).map(bytes => new String(bytes.toArray)).getOrElse(throw new IllegalArgumentException("No query found in POST request."))
              Await.result(select(project, task, query, defaultGraphUri, namedGraphUri)(request), Duration.Inf)
            case Some(FORM) =>
              val decoded = request.body.asFormUrlEncoded.getOrElse(throw new IllegalArgumentException("No content found in POST request."))
              val query = decoded.get("query").flatMap(q => q.headOption).getOrElse(throw new IllegalArgumentException("No 'query' parameter found in POST request."))
              val defGraph = decoded.getOrElse("default-graph-uri", Seq())
              val namedGraph = decoded.getOrElse("named-graph-uri", Seq())
              Await.result(select(project, task, query, defGraph.toList, namedGraph.toList)(request), Duration.Inf)
            case _ => ErrorResult(SparqlContentNegotiationError(request.contentType))
          }
    }
  }

  def ask(project: String, task: String, query: String = "", defaultGraphUri: List[String] = List(), namedGraphUri: List[String] = List()): Action[AnyContent] =
    RequestUserContextAction {
      implicit request =>
      implicit userContext =>
        val updatedQuery = checkGraphParams(query, defaultGraphUri, namedGraphUri)
        val context = Context.get[GenericDatasetSpec](project, task, request.path)
        implicit val jsonWriteContext: WriteContext[JsValue] = getJsonWriteContext(context)

        context.task.data.plugin match {
          case rdf: RdfDataset =>
            val sparqlEndpoint = rdf.sparqlEndpoint
            val queryResults = sparqlEndpoint.ask(updatedQuery)
            Ok(SparqlResultSerializer.write(queryResults))
          case _ =>
            ErrorResult(BadUserInputException("This is not an RDF-Dataset."))
        }
      }

  def postAsk(project: String, task: String, defaultGraphUri: List[String] = List(), namedGraphUri: List[String] = List()): Action[AnyContent] = {
    RequestUserContextAction {
      implicit request =>
        implicit userContext =>
          request.contentType match{
            case Some(SparqlProtocolApi.SPARQL) =>
              val query = request.body.asRaw.flatMap(_.asBytes()).map(bytes => new String(bytes.toArray)).getOrElse(throw new IllegalArgumentException("No query found in POST request."))
              Await.result(ask(project, task, query, defaultGraphUri, namedGraphUri)(request), Duration.Inf)
            case Some(FORM) =>
              val decoded = request.body.asFormUrlEncoded.getOrElse(throw new IllegalArgumentException("No content found in POST request."))
              val query = decoded.get("query").flatMap(q => q.headOption).getOrElse(throw new IllegalArgumentException("No 'query' parameter found in POST request."))
              val defGraph = decoded.getOrElse("default-graph-uri", Seq())
              val namedGraph = decoded.getOrElse("named-graph-uri", Seq())
              Await.result(ask(project, task, query, defGraph.toList, namedGraph.toList)(request), Duration.Inf)
            case _ => ErrorResult(SparqlContentNegotiationError(request.contentType))
          }
    }
  }
}


object SparqlProtocolApi{
  // the SPARQL mime type
  val SPARQL = "application/sparql-query"

  case class SparqlContentNegotiationError(contentType: Option[String]) extends RequestException(
    s"${contentType.getOrElse("No content type ")} found, SPARQL protocol 1.1 only supports $SPARQL and ${MimeTypes.FORM}", None){
    /**
      * A short description of the error type, e.g, "Task not found".
      * Should be the same for all instances of the error type.
      */
    override def errorTitle: String = s"Unsupported Content Type"

    /**
      * The HTTP error code that fits best to the given error type.
      */
    override def httpErrorCode: Option[Int] = Some(Status.NOT_ACCEPTABLE)
  }
}
