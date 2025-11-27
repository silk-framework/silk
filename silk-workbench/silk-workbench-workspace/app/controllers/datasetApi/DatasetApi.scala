package controllers.datasetApi

import controllers.core.UserContextActions
import controllers.core.util.ControllerUtilsTrait
import controllers.datasetApi.doc.DatasetApiDoc
import controllers.datasetApi.payloads.DatasetCharacteristicsPayload._
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{Content, ExampleObject, Schema}
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, InjectedController}

import javax.inject.Inject

/**
  * REST API for dataset tasks.
  */
@Tag(name = "Datasets", description = "Dataset-specific REST endpoint.")
class DatasetApi @Inject() () extends InjectedController with UserContextActions with ControllerUtilsTrait {
  @Operation(
    summary = "Retrieve dataset characteristics",
    description = "Retrieve the characteristics of a dataset.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Success",
        content = Array(
          new Content(
            mediaType = "application/json",
            examples = Array(new ExampleObject(DatasetApiDoc.datasetCharacteristicsExampleJson))
          )
        )
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the specified project or dataset has not been found."
      )
    )
  )
  def datasetCharacteristics(@Parameter(
                               name = "projectId",
                               description = "The project identifier",
                               required = true,
                               in = ParameterIn.PATH,
                               schema = new Schema(implementation = classOf[String])
                             )
                             projectId: String,
                             @Parameter(
                               name = "datasetId",
                               description = "The dataset identifier",
                               required = true,
                               in = ParameterIn.PATH,
                               schema = new Schema(implementation = classOf[String])
                             )
                             datasetId: String): Action[AnyContent] = UserContextAction { implicit userContext =>
      val (_, dataset) = projectAndTask[GenericDatasetSpec](projectId, datasetId)
      val datasetCharacteristics = dataset.data.characteristics
      Ok(Json.toJson(datasetCharacteristics))
  }

  @Operation(
    summary = "Clear dataset",
    description = "Clears the content of a dataset. What will actually be done depends on the dataset's implementation, e.g. deleting the referenced file etc.",
    responses = Array(
      new ApiResponse(
        responseCode = "201",
        description = "Success"
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the specified project or dataset has not been found."
      )
    )
  )
  def clearDataset(@Parameter(
                     name = "projectId",
                     description = "The project identifier",
                     required = true,
                     in = ParameterIn.PATH,
                     schema = new Schema(implementation = classOf[String])
                   )
                   projectId: String,
                   @Parameter(
                     name = "datasetId",
                     description = "The dataset identifier",
                     required = true,
                     in = ParameterIn.PATH,
                     schema = new Schema(implementation = classOf[String])
                   )
                   datasetId: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    val (_, dataset) = projectAndTask[GenericDatasetSpec](projectId, datasetId)
    val sink = dataset.data.entitySink
    sink.clear(force = true)
    NoContent
  }
}