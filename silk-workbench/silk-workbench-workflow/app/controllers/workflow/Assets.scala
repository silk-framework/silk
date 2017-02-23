package controllers.workflow

import javax.inject.Inject
import play.api.http.HttpErrorHandler

class Assets @Inject() (errorHandler: HttpErrorHandler) extends controllers.AssetsBuilder(errorHandler)
