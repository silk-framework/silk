package controllers.workspace

import controllers.core.RequestUserContextAction
import javax.inject.Inject
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.dataset.rdf.RdfDataset
import org.silkframework.runtime.serialization.WriteContext
import org.silkframework.runtime.validation.BadUserInputException
import org.silkframework.workbench.Context
import org.silkframework.workbench.utils.ErrorResult
import play.api.libs.json.JsValue
import play.api.mvc.{Action, AnyContent, InjectedController}

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
}
