package org.silkframework.plugins.dataset.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.JsonNodeDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class DuplicateToArrayJsonNodeDeserializer extends JsonNodeDeserializer {

    @Override
    protected void _handleDuplicateField(JsonParser p, DeserializationContext ctxt,
                                         JsonNodeFactory nodeFactory, String fieldName, ObjectNode objectNode,
                                         JsonNode oldValue, JsonNode newValue) {
        ArrayNode node;
        if(oldValue instanceof ArrayNode){
            node = (ArrayNode) oldValue;
            node.add(newValue);
        } else {
            node = nodeFactory.arrayNode();
            node.add(oldValue);
            node.add(newValue);
        }
        objectNode.set(fieldName, node);
    }
}