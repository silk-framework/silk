package controllers.workspace

import controllers.AssetsMetadata
import play.api.Environment

import javax.inject.Inject
import play.api.http.HttpErrorHandler

class Assets @Inject() (errorHandler: HttpErrorHandler, meta: AssetsMetadata, env: Environment) extends controllers.AssetsBuilder(errorHandler, meta, env)