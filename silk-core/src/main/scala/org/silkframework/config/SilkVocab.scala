package org.silkframework.config

/**
  * Created on 8/2/16.
  */
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

  // Triple input/output schema vocabulary

  val TripleSchemaType: String = namespace + "/TripleSchemaType"

  val SparqlEndpointSchemaType: String = namespace + "/SparqlEndpointSchemaType"

  val DatasetResourceSchemaType: String = namespace + "/DatasetResourceSchemaType"

  val tripleSubject: String = namespace + "tripleSubject"
  val triplePredicate: String = namespace + "triplePredicate"
  val tripleObject: String = namespace + "tripleObject"
  val tripleObjectValueType: String = namespace + "tripleObjectValueType"
}
