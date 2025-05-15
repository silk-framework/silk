package controllers.swaggerUi

import controllers.AssetsMetadata
import play.api.Environment
import play.api.http.HttpErrorHandler

import javax.inject.Inject

class SwaggerUiAssets @Inject() (errorHandler: HttpErrorHandler, meta: AssetsMetadata, env: Environment) extends controllers.AssetsBuilder(errorHandler, meta, env)
