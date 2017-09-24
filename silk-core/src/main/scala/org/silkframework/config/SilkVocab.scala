package org.silkframework.config

/**
  * Created on 8/2/16.
  */
object SilkVocab {
  val namespace = "http://silkframework.org/vocab/"

  val taskSpecNamespace = namespace + "taskSpec/"

  // REST Task vocabulary

  val RestTaskData = taskSpecNamespace + "RestTaskData"

  val RestTaskPropertyURL = RestTaskData + "/propertyURL"

  val RestTaskPropertyContent = RestTaskData + "/propertyContent"

  val RestTaskResult = taskSpecNamespace + "RestTaskResult"

  val RestTaskResultContentType = RestTaskResult + "/propertyContentType"

  val RestTaskResultUrl = RestTaskResult + "/url"

  val RestTaskResultResponseBody = RestTaskResult + "/responseBody"

  // Triple input/output schema vocabulary

  val TripleSchemaType = namespace + "/TripleSchemaType"

  val SparqlEndpointSchemaType = namespace + "/SparqlEndpointSchemaType"

  val tripleSubject = namespace + "tripleSubject"
  val triplePredicate = namespace + "triplePredicate"
  val tripleObject = namespace + "tripleObject"
  val tripleObjectValueType = namespace + "tripleObjectValueType"
}
