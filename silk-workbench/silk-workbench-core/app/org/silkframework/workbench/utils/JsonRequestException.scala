package org.silkframework.workbench.utils

import play.api.libs.json.JsObject

/**
  * Request exception that allows to include additional JSON into the response.
  *
  * @param msg The detailed error description.
  * @param cause The optional cause of this exception.
  *
  */
trait JsonRequestException { RequestException =>

  /**
    * Json that will be included in addition to the HTTP Problem details JSON.
    * Note that using reserved HTTP Problem details fields (type, title, detail) would overwrite the generated ones.
    */
  def additionalJson: JsObject

}
