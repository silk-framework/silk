package org.silkframework.config

/** Vocabulary used by tasks, e.g. for output entity schemata. */
object SilkVocab {
  val namespace = "http://silkframework.org/vocab/"

  val taskSpecNamespace: String = namespace + "taskSpec/"

  // REST Task vocabulary

  val RestTaskData: String = taskSpecNamespace + "RestTaskData"

  val RestTaskPropertyURL: String = RestTaskData + "/propertyURL"

  val RestTaskPropertyContent: String = RestTaskData + "/propertyContent"

  val RestTaskResult: String = taskSpecNamespace + "RestTaskResult"

  val RestTaskResultContentType: String = RestTaskResult + "/propertyContentType"

  val RestTaskResultUrl: String = RestTaskResult + "/url"

  val RestTaskResultResponseBody: String = RestTaskResult + "/responseBody"

  // Empty table
  val EmptySchemaType: String = namespace + "EmptySchemaType"

  val internalUser: String = namespace + "internalUser"
}
