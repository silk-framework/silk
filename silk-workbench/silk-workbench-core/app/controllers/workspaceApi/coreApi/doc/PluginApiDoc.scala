package controllers.workspaceApi.coreApi.doc

object PluginApiDoc {

  final val taskPluginsExampleJson =
    """
      {
        "multiCsv" : {
          "title" : "Multi CSV ZIP",
          "categories" : [ "file" ],
          "description" : "Reads from or writes to multiple CSV files from/to a single ZIP file.",
          "markdownDocumentation": "# Some markdown documentations"
        },
        "csv" : {
          "title" : "CSV",
          "categories" : [ "file" ],
          "description" : "Read from or write to an CSV file."
        }
      }
    """

  final val operatorPluginsExampleJson =
    """
{
  ...
  "concat": {
    "categories": [
        "Combine"
    ],
    "description": "Concatenates strings from multiple inputs.",
    "pluginId": "concat",
    "pluginType": "TransformOperator",
    "properties": {
        "glue": {
            "advanced": false,
            "description": "Separator to be inserted between two concatenated strings.",
            "parameterType": "string",
            "title": "Glue",
            "type": "string",
            "value": "",
            "visibleInDialog": true
        },
        "missingValuesAsEmptyStrings": {
            "advanced": false,
            "description": "Handle missing values as empty strings.",
            "parameterType": "boolean",
            "title": "Missing values as empty strings",
            "type": "string",
            "value": "false",
            "visibleInDialog": true
        }
    },
    "required": [],
    "title": "Concatenate",
    "type": "object"
  }
  ...
}
    """

  final val pluginDescriptionExampleJson =
    """
      {
        "title" : "Transform",
        "categories" : [ "Transform" ],
        "description" : "A transform task defines a mapping from a source structure to a target structure.",
        "taskType" : "Transform",
        "type" : "object",
        "pluginId": "transform",
        "properties" : {
          "selection" : {
            "title" : "Input task",
            "description" : "The source from which data will be transformed when executed as a single task outside of a workflow.",
            "type" : "object",
            "parameterType" : "objectParameter",
            "value" : null,
            "advanced" : false,
            "visibleInDialog" : true,
            "pluginId" : "datasetSelectionParameter",
            "properties" : {
              "inputId" : {
                "title" : "Dataset",
                "description" : "The dataset to select.",
                "type" : "string",
                "parameterType" : "identifier",
                "value" : null,
                "advanced" : false,
                "visibleInDialog" : true,
                "autoCompletion" : {
                  "allowOnlyAutoCompletedValues" : true,
                  "autoCompleteValueWithLabels" : true,
                  "autoCompletionDependsOnParameters" : [ ]
                }
              },
              "typeUri" : {
                "title" : "Type",
                "description" : "The type of the dataset. If left empty, the default type will be selected.",
                "type" : "string",
                "parameterType" : "uri",
                "value" : null,
                "advanced" : false,
                "visibleInDialog" : true,
                "autoCompletion" : {
                  "allowOnlyAutoCompletedValues" : false,
                  "autoCompleteValueWithLabels" : false,
                  "autoCompletionDependsOnParameters" : [ ]
                }
              },
              "restriction" : {
                "title" : "Restriction",
                "description" : "Additional restrictions on the enumerated entities. If this is an RDF source, use SPARQL patterns that include the variable ?a to identify the enumerated entities, e.g. ?a foaf:knows <http://example.org/SomePerson>",
                "type" : "string",
                "parameterType" : "restriction",
                "value" : "",
                "advanced" : false,
                "visibleInDialog" : true
              }
            }
          },
          "mappingRule" : {
            "title" : "mapping rule",
            "description" : "",
            "type" : "object",
            "parameterType" : "objectParameter",
            "value" : {
              "type" : "root",
              "id" : "root",
              "rules" : {
                "uriRule" : null,
                "typeRules" : [ ],
                "propertyRules" : [ ]
              },
              "metadata" : {
                "label" : "Root Mapping"
              }
            },
            "advanced" : false,
            "visibleInDialog" : false
          },
          "output" : {
            "title" : "Output dataset",
            "description" : "An optional dataset where the transformation results should be written to when executed as single task outside of a workflow.",
            "type" : "string",
            "parameterType" : "option[identifier]",
            "value" : "",
            "advanced" : false,
            "visibleInDialog" : true,
            "autoCompletion" : {
              "allowOnlyAutoCompletedValues" : true,
              "autoCompleteValueWithLabels" : true,
              "autoCompletionDependsOnParameters" : [ ]
            }
          }
        },
        "required" : [ "selection" ]
      }
    """

  final val pluginJsonDescription =
    """
Contains the typical meta data of a plugin like title, categories and description.
The 'taskType' property is optional and specifies the task type a task related plugin, e.g. workflow, dataset etc., belongs to.
The task type must be specified when creating tasks via the generic /tasks endpoint.
The JSON schema part of the plugin parameters is described in the 'properties' object. Besides title
and description each parameter has the JSON type, which can only be "string" or "object" at the moment.
The 'parameterType' specifies the internal data type. For "object" types this can be ignored, for "string"
parameter types this gives a hint at what kind of UI widget is appropriate and what kind of validation could be applied.
The 'value' property gives the default value when this parameter is not specified.
The 'advanced' property marks the parameter as advanced and acts as a hint that this parameter should
be somehow handled differently by the UI.
If the 'visibleInDialog' property is set to false, then this parameter should not be set from a creation
or update dialog. Usually this parameter is complex and is modified in special editors, e.g. the mapping editor.
The pluginId property specifies the ID of the plugin and is also set for all plugin parameters that
are plugins themselves. The plugin ID is needed, e.g. for the auto-completion of parameter values.
A parameter can have an autoCompletion property that specifies how a parameter value can or should be auto-completed.
If allowOnlyAutoCompletedValues is set to true then the UI must make sure that only values from the auto-completion
are considered as valid.
If autoCompleteValueWithLabels is set to true, then the auto-completion values might have a label in addition
to the actual value. Only the label should be presented to the user then.
The autoCompletionDependsOnParameters array specifies the values of parameters from the same object,
a specific parameter depends on. These must be send in the auto-completion request in the same order.
"""

  final val pluginUsagesExample =
    """
      [
        {
          "project": "projectId",
          "task": "taskId",
          "link": "{editor URL}"
        }
      ]
    """

  final val resourceBasedPluginIdsExample =
    """
      ["file", "csv", "xml"]
    """
}
