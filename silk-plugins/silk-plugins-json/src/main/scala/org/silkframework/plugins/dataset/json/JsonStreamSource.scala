package org.silkframework.plugins.dataset.json

import org.silkframework.config.Prefixes
import org.silkframework.entity.EntitySchema
import org.silkframework.execution.EntityHolder
import org.silkframework.execution.local.GenericEntityTable
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource.Resource
import org.silkframework.util.Identifier
import play.api.libs.json.JsValue



class JsonStreamSource(taskId: Identifier, resource: Resource, input: JsValue, basePath: String, uriPattern: String) extends JsonSource(taskId, input, basePath, uriPattern) {

  override def retrieve(entitySchema: EntitySchema, limit: Option[Int])(implicit userContext: UserContext, prefixes: Prefixes): EntityHolder = {
    val reader = new JsonStreamReader(taskId, resource, basePath, uriPattern)
    val entities = reader.retrieve(entitySchema, limit)
    GenericEntityTable(entities, entitySchema, underlyingTask)
  }

}

object JsonStreamSource {

}
