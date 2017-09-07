package org.silkframework.workbench.utils

import java.net.HttpURLConnection

import org.silkframework.runtime.validation.ClientRequestException

case class NotAcceptableException(msg: String) extends ClientRequestException(msg, None, HttpURLConnection.HTTP_NOT_ACCEPTABLE, "Not Acceptable")
