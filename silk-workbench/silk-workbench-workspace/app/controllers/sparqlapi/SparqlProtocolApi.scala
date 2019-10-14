package controllers.sparqlapi

import controllers.core.RequestUserContextAction
import controllers.sparqlapi.SparqlProtocolApi._
import javax.inject.Inject
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.dataset.rdf.RdfDataset
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.serialization.WriteContext
import org.silkframework.runtime.validation.{BadUserInputException, RequestException}
import org.silkframework.workbench.Context
import org.silkframework.workbench.utils.ErrorResult
import play.api.http.{MediaRange, MimeTypes, Status}
import play.api.libs.json.JsValue
import play.api.mvc.{Action, AnyContent, InjectedController, Result}

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.xml.Node

/**
  * This API implements a SPARQL 1.1 protocol conform SPARQL endpoint for each [[RdfDataset]]
  * Thereby instantaneous SPARQL access via REST and SPARQL SERVICE keyword
  * to every [[RdfDataset]] is available without pushing triples to an active triple store.
  * FIXME CONSTRUCT and DESCRIBE queries are currently not supported, but should be easy to implement
  */
class SparqlProtocolApi @Inject() () extends InjectedController {

  private def getWriteContext[Ser](context: Context[GenericDatasetSpec]): WriteContext[Ser] = WriteContext[Ser](
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

  def select(project: String, task: String, query: String = "", defaultGraphUri: List[String] = List(), namedGraphUri: List[String] = List()): Action[AnyContent] = RequestUserContextAction {
    implicit request =>
    implicit userContext =>
    val context = Context.get[GenericDatasetSpec](project, task, request.path)
    val updatedQuery = checkGraphParams(query, defaultGraphUri, namedGraphUri)
    executeQuery(updatedQuery, request.acceptedTypes, context)
  }

  def postSelect(project: String, task: String, defaultGraphUri: List[String] = List(), namedGraphUri: List[String] = List()): Action[AnyContent] = {
    RequestUserContextAction {
      implicit request =>
      implicit userContext =>
      request.contentType match{
        case Some(SPARQLQUERY) =>
          val query = request.body.asRaw.flatMap(_.asBytes()).map(bytes => new String(bytes.toArray)).getOrElse(throw new IllegalArgumentException("No query found in POST request."))
          Await.result(select(project, task, query, defaultGraphUri, namedGraphUri)(request), Duration.Inf)
        case Some(FORM) =>
          val decoded = request.body.asFormUrlEncoded.getOrElse(throw new IllegalArgumentException("No content found in POST request."))
          val query = decoded.get("query").flatMap(q => q.headOption).getOrElse(throw new IllegalArgumentException("No 'query' parameter found in POST request."))
          val defGraph = decoded.getOrElse("default-graph-uri", Seq())
          val namedGraph = decoded.getOrElse("named-graph-uri", Seq())
          Await.result(select(project, task, query, defGraph.toList, namedGraph.toList)(request), Duration.Inf)
        case _ => ErrorResult(SparqlSparqlUnsupportedMediaType(request.contentType))
      }
    }
  }

  private def executeQuery(query: String, accepts: Seq[MediaRange], context: Context[GenericDatasetSpec])(implicit uc: UserContext): Result = {
    val acceptableMediaTypes = accepts.map(a => (a.mediaType + "/" + a.mediaSubType, a.qValue.map(_.doubleValue()).getOrElse(0d)))
      .filter(a => SparqlProtocolApi.SupportedMediaTyped.contains(a._1))
      .sortBy(a => (a._2, a._1))( Ordering.Tuple2(Ordering.Double.reverse, Ordering.String))
    val chosenMediaType = if(acceptableMediaTypes.isEmpty) accepts.headOption.map(a => a.mediaType + "/" + a.mediaSubType) else acceptableMediaTypes.headOption.map(_._1)

    context.task.data.plugin match {
      case rdf: RdfDataset =>
        val sparqlEndpoint = rdf.sparqlEndpoint
        val queryResults = SparqlQueryType.determineSparqlQueryType(query) match{
          case SparqlQueryType.ASK => sparqlEndpoint.ask(query)
          case SparqlQueryType.SELECT => sparqlEndpoint.select(query)
          case typ: SparqlQueryType.Val => return ErrorResult(SparqlUnsupportedQueryTypeError(Some(typ)))
        }

        chosenMediaType match{
          case Some(SPARQLJSONRESULT) => Ok(SparqlResultJsonSerializers.write(queryResults)(getWriteContext[JsValue](context)))
          case Some(SPARQLXMLRESULT) => Ok(SparqlResultXmlSerializers.write(queryResults)(getWriteContext[Node](context)))
          case Some(unsupported) => ErrorResult(SparqlContentNegotiationError(Some(unsupported))) //unsupported!
          case None | Some(MimeTypes.TEXT) | Some(MimeTypes.TEXT) =>
            Ok(SparqlResultXmlSerializers.write(queryResults)(getWriteContext[Node](context)))    // by default we return xml, which also includes text/plain and text/html
        }
      case _ => ErrorResult(BadUserInputException("This is not an RDF-Dataset."))
    }
  }
}

object SparqlProtocolApi{
  // the SPARQL query media type
  val SPARQLQUERY = "application/sparql-query"
  // the SPARQL Json Result Set media type
  val SPARQLJSONRESULT = "application/sparql-results+json"
  // the SPARQL Json Result Set media type
  val SPARQLXMLRESULT = "application/sparql-results+xml"

  /* media types supported for SPARQL result sets */
  val SupportedMediaTyped: Seq[String] = Seq(SPARQLJSONRESULT, SPARQLXMLRESULT)

  case class SparqlSparqlUnsupportedMediaType(contentType: Option[String]) extends RequestException(
    s"${contentType.getOrElse("No content type ")} found, SPARQL protocol 1.1 only supports $SPARQLQUERY and ${MimeTypes.FORM}", None){
    override def errorTitle: String = s"Unsupported Media Type"
    override def httpErrorCode: Option[Int] = Some(Status.UNSUPPORTED_MEDIA_TYPE)
  }

  case class SparqlContentNegotiationError(contentType: Option[String]) extends RequestException(
    s"${contentType.getOrElse("No content type ")} is not supported. Currently these media types are supported: ${SupportedMediaTyped.mkString(", ")}", None){
    override def errorTitle: String = s"Unsuccessful Content Negotiation."
    override def httpErrorCode: Option[Int] = Some(Status.NOT_ACCEPTABLE)
  }

  case class SparqlUnsupportedQueryTypeError(queryType: Option[SparqlQueryType.Val]) extends RequestException(
    s"${queryType.map(_.name).getOrElse("Unknown query type ")} is unsupported. Currently these query types are supported: SELECT, ASK", None){
    override def errorTitle: String = s"Unsupported Query Type."
    override def httpErrorCode: Option[Int] = Some(Status.NOT_IMPLEMENTED)
  }
}
