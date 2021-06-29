package controllers.transform.doc

object TransformTaskApiDoc {

  final val transformRulesJsonExample =
    """
      {
        "type" : "root",
        "id" : "root",
        "rules" : {
          "uriRule" : {
            "type" : "uri",
            "id" : "uri",
            "pattern" : "http://example.org/{PersonID}"
          },
          "typeRules" : [ {
            "type" : "type",
            "id" : "explicitlyDefinedId",
            "typeUri" : "target:Person"
          } ],
          "propertyRules" : [ {
            "type" : "direct",
            "id" : "directRule",
            "sourcePath" : "/source:name",
            "mappingTarget" : {
              "uri" : "target:name",
              "valueType" : {
                "nodeType" : "StringValueType"
              },
              "isBackwardProperty": false
            }
          }, {
            "type" : "object",
            "id" : "objectRule",
            "sourcePath" : "/source:address",
            "mappingTarget" : {
              "uri" : "target:address",
              "valueType" : {
                "nodeType" : "UriValueType"
              },
              "isBackwardProperty": false
            },
            "rules" : {
              "uriRule" : null,
              "typeRules" : [ ],
              "propertyRules" : [ ]
            }
          } ]
        }
      }
    """

  final val transformRulesXmlExample =
    """<TransformRules>
  <TransformRule name="type1">
    <TransformInput id="generateType" function="constantUri">
      <Param name="value" value="http://dbpedia.org/ontology/Film"/>
    </TransformInput>
    <MappingTarget uri="http://www.w3.org/1999/02/22-rdf-syntax-ns#type" isBackwardProperty="false">
      <ValueType nodeType="UriValueType"/>
    </MappingTarget>
  </TransformRule>
  <TransformRule name="sourcePath1">
    <TransformInput id="lowerCase1" function="lowerCase">
      <Input id="sourcePath1" path="/&lt;http://data.linkedmdb.org/resource/movie/director&gt;/&lt;http://www.w3.org/2000/01/rdf-schema#label&gt;">
      </Input>
    </TransformInput>
    <MappingTarget uri="http://director" isBackwardProperty="false">
      <ValueType nodeType="StringValueType"/>
    </MappingTarget>
  </TransformRule>
</TransformRules>"""

  final val transformRuleJsonExample =
    """
      {
        "type" : "object",
        "id" : "objectRule",
        "sourcePath" : "/source:address",
        "mappingTarget" : {
          "uri" : "target:address",
          "valueType" : {
            "nodeType" : "UriValueType"
          },
          "isBackwardProperty": false
        },
        "rules" : {
          "uriRule" : null,
          "typeRules" : [ ],
          "propertyRules" : [ ]
        }
      }
    """

  final val transformRuleXmlExample =
    """<TransformRule name="sourcePathx">
  <TransformInput id="lowerCase1" function="lowerCase">
    <Input id="sourcePath1" path="/&lt;http://www.w3.org/2000/01/rdf-schema#label&gt;"/>
  </TransformInput>
  <MappingTarget uri="http://www.w3.org/2000/01/rdf-schema#label" isBackwardProperty="false">
    <ValueType nodeType="AutoDetectValueType"/>
  </MappingTarget>
</TransformRule>"""

  final val updateRuleRequestExample =
    """
      {
        "rules": {
          "uriRule": {
            "type": "uri",
            "pattern": "http://example.org/{PersonID}"
          },
          "typeRules": [
            {
              "type": "type",
              "id": "explicitlyDefinedId",
              "typeUri": "target:Person"
            }
          ]
        }
      }
    """

  final val updateRuleResponseExample =
    """
      {
        "type" : "root",
        "id" : "root",
        "rules" : {
          "uriRule" : {
            "type" : "uri",
            "id" : "uri",
            "pattern" : "http://example.org/{PersonID}"
          },
          "typeRules" : [ {
            "type" : "type",
            "id" : "explicitlyDefinedId",
            "typeUri" : "target:Person"
          } ],
          "propertyRules" : [ ]
        }
      }
    """

  final val appendRuleRequestExample =
    """
      {
        "type": "direct",
        "sourcePath": "/source:name",
        "mappingTarget": {
          "uri": "target:name",
          "valueType": {
            "nodeType": "StringValueType"
          }
        }
      }
    """

  final val appendRuleResponseExample =
    """
      {
        "type" : "direct",
        "id" : "directRule",
        "sourcePath" : "/source:name",
        "mappingTarget" : {
          "uri" : "target:name",
          "valueType" : {
            "nodeType" : "StringValueType"
          }
        }
      }
    """

  final val transformInputExample =
    """
      <Transform>
        <DataSources>
          <Dataset id="Some Id which gets ignored anyway">
            <DatasetPlugin type="json"> <!-- or any other dataset plugin type, e.g. xml, rdf -->
              <Param name="file" value="postResource"/> <!-- This references the resource following next -->
              <Param name="basePath" value="bp"/> <!-- Other parameters specific to this dataset plugin -->
              <Param name="uriPattern" value="https://www.example.com/resource/{id}"/>
            </DatasetPlugin>
          </Dataset>
        </DataSources>
        <resource name="postResource">
          {
            ...
          }
        </resource>
      </Transform>
    """
}
