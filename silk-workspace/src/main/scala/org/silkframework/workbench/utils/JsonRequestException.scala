package org.silkframework.workbench.utils

import play.api.libs.json.JsObject

/**
  * Request exception that allows to include additional JSON into the response.
  *
  */
trait JsonRequestException { RequestException =>

  /**
    * JSON that will be included in addition to the HTTP Problem details JSON.
    * Note that using reserved HTTP Problem details fields (type, title, detail) would overwrite the generated ones.
    */
  def additionalJson: JsObject

}
