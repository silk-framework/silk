package org.silkframework.plugins.dataset.json

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.{DeserializationContext, JsonNode}
import com.fasterxml.jackson.databind.deser.std.JsonNodeDeserializer
import com.fasterxml.jackson.databind.node.{ArrayNode, JsonNodeFactory, ObjectNode}

class DuplicateNodeDeserializer extends JsonNodeDeserializer {

  @Override
  override def _handleDuplicateField( p: JsonParser, c: DeserializationContext, nf: JsonNodeFactory, field: String,
                                      objectNode: ObjectNode, oldVal: JsonNode, newVal: JsonNode): Unit = {
     val newNode: ArrayNode =
       if (oldVal.isInstanceOf[ArrayNode]) {
         val node = oldVal.asInstanceOf[ArrayNode]
         node.add(newVal)
         node
       } else {
         val node = nf.arrayNode
         node.add(oldVal)
         node.add(newVal)
         node
       }
       objectNode.set(field, newNode)
  }


}
