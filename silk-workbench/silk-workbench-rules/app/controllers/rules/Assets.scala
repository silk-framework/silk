package controllers.rules

import controllers.AssetsMetadata
import javax.inject.Inject
import play.api.http.HttpErrorHandler

class Assets @Inject() (errorHandler: HttpErrorHandler, meta: AssetsMetadata) extends controllers.AssetsBuilder(errorHandler, meta)
