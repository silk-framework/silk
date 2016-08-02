package org.silkframework.config

/**
  * Created on 8/2/16.
  */
object SilkVocab {
  val namespace = "http://silkframework.org/vocab/"

  val taskSpecNamespace = namespace + "taskSpec/"

  val RestTaskData = taskSpecNamespace + "RestTaskData"

  val RestTaskPropertyURL = RestTaskData + "/propertyURL"

  val RestTaskPropertyContent = RestTaskData + "/propertyContent"

  val RestTaskResult = taskSpecNamespace + "RestTaskResult"

  val RestTaskPropertyContentType = RestTaskData + "/propertyContentType"
}
