package controllers.openapi

import controllers.AssetsMetadata
import play.api.http.HttpErrorHandler

import javax.inject.Inject

class Assets @Inject()(errorHandler: HttpErrorHandler, meta: AssetsMetadata) extends controllers.AssetsBuilder(errorHandler, meta)
