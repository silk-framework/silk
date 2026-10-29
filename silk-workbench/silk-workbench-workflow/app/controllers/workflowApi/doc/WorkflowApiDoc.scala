package controllers.workflowApi.doc

object WorkflowApiDoc {

  // Shared between the synchronous and the asynchronous variant, which only differ in how the output type is chosen and what is returned.
  private final val variableWorkflowCompatibility =
    """Compatible with all workflows that contain at most one replaceable (variable) input dataset and at most one replaceable output dataset.
Both are optional and each of them may be used several times in the same workflow."""

  private final val variableWorkflowInputSection =
    """**Input entity:** If the workflow has a replaceable input dataset, a single entity is built from the provided request parameters and injected into it.
There must be at least one form or query parameter specified in the request. The reserved control parameters 'output:type', 'config-*' and 'variable-*'
are never part of the input entity. If empty entities as input must be supported, a POST request with an empty JSON or XML object/element should be used.
For the text formats JSON, XML and CSV, the POST body can instead contain arbitrary content of the corresponding dataset type, e.g. several entities; the content type must be set accordingly.
Binary formats (e.g. Excel, binary files) and the formats of other file-based dataset plugins must be uploaded as a file via multipart/form-data, see the request body.
If the workflow has no replaceable input dataset, the input content is ignored; execution variables and the content type check still apply."""

  private final val variableWorkflowConfigSection =
    """**Dataset configuration:** Query parameters with the prefixes 'config-dataSourceConfig-' and 'config-dataSinkConfig-' override parameters of the
replaceable input and output dataset, e.g. 'config-dataSourceConfig-separator=;' sets the separator of a CSV input.
'config-general-autoConfig=true' enables auto-configuration of the source dataset, e.g. to detect a non-comma separator of a CSV input."""

  private final val variableWorkflowExecutionVariablesSection =
    """**Execution variables:** Execution variables can be provided as query parameters with the reserved prefix 'variable-', e.g. 'variable-myVar=some value',
independent of the request content type, and for JSON payloads additionally under the reserved top-level key 'executionVariables' as a flat name/value map.
The provided values override the default values of the workflow's execution variables. Variables that no task of the workflow declares are accepted as well.
Missing required execution variables are not checked by the request; a task that references an unset variable fails during the workflow execution."""

  // Concatenated with '+' so that the value stays a compile-time constant usable in annotations.
  final val variableWorkflowResultPostDescription =
    """Executes a workflow with parameters from the request query string or form URL encoded body and waits for its completion.

""" + variableWorkflowCompatibility + "\n\n" + variableWorkflowInputSection + """

**Output:** The output data type is specified via the ACCEPT header. The result is returned as the requested mime type,
the content is the file content of the corresponding dataset, e.g. XML, CSV etc.
Alternatively, the 'output:type' query parameter selects the output type by dataset plugin id and takes precedence over the ACCEPT header.
If the workflow has no replaceable output dataset, both are ignored and no content is returned.

""" + variableWorkflowConfigSection + "\n\n" + variableWorkflowExecutionVariablesSection

  final val variableWorkflowAsyncRetentionNote =
    """**Retention of executions:** Only a limited number of execution instances per workflow is kept (config key 'org.silkframework.runtime.activity.concurrentExecutions', default 20).
When the limit is reached, the oldest finished instance is dropped together with its result.
Consumed results should therefore be removed by the client via the 'Remove a workflow execution instance' endpoint of the Workflows API, passing 'instanceId' as 'executionId'."""

  final val variableWorkflowResultPostDescriptionAsync =
    """Executes a workflow with parameters from the request query string or form URL encoded body, without waiting for its completion.
It returns the id of the executing activity ('activityId') and the id of this particular execution ('instanceId').
The execution status can be polled with the 'Get activity status' endpoint of the Activities API,
passing the workflow id as 'task', 'activityId' as 'activity' and 'instanceId' as 'instance'.
Once the execution has finished, its result can be retrieved via the 'Parameterized workflow execution result (asynchronous)' endpoint, passing 'instanceId' as query parameter.
Failures of the execution are reported by the activity status and by the result endpoint, not by this endpoint.

""" + variableWorkflowCompatibility + "\n\n" + variableWorkflowInputSection + """

**Output:** The output type of the replaceable output dataset is selected with the 'output:type' query parameter or, if it is missing, with the ACCEPT header.
The result endpoint serves the file content of that dataset.

""" + variableWorkflowConfigSection + "\n\n" + variableWorkflowExecutionVariablesSection + "\n\n" + variableWorkflowAsyncRetentionNote

  final val variableWorkflowRequestBodyDescription =
    """The contents of the replaceable input dataset.
For JSON payloads, the top-level key 'executionVariables' is reserved for execution-variable overrides: it must be a flat name/value string map and never becomes part of the input entity."""

  final val outputTypeParameterDescription =
    "The id of the file-based dataset plugin that writes the output of the replaceable output dataset, e.g. 'json', 'xml', 'csv', 'file' (N-Triples), 'excel' or 'binaryFile'. " +
      "Takes precedence over the ACCEPT header, which is used if this parameter is missing. Ignored if the workflow has no replaceable output dataset."

  final val variableWorkflowResultGetDescription =
    """For a GET request the parameter and values are provided via the query string of the URL.
In order to provide multiple values for an input property, the same query parameter should be used multiple times,
e.g. `inputProp=value+1&inputProp=value+2`.
The responses are the same as for the POST request."""

  final val variableWorkflowRequestFormsExample =
    """
      {
        "param1": "param1 value",
        "param2": ["param2 value", "param2 second value"]
      }
    """

  final val variableWorkflowRequestJsonExample =
    """
      {
        "param1": "param1 value",
        "param2": ["param2 value", "param2 second value"]
      }
    """

  final val variableWorkflowRequestXmlExample =
    """
      <SomeRoot>
        <param1>param1 value</param1>
        <param2>param2 value</param2>
        <param2>param2 second value</param2>
      </SomeRoot>
    """

  final val variableWorkflowRequestCsvExample =
    """param1,param2
param1 value,param2 value"""

  final val variableWorkflowResponseXmlExample =
    """
      <?xml version="1.0" encoding="UTF-8" standalone="no"?>
      <Root>
        <Entity>
          <targetProp1>val 1</targetProp1>
          <targetProp2>val 2</targetProp2>
        </Entity>
      </Root>
    """

  final val variableWorkflowResponseNTriplesExample =
    """<urn:instance:variable_workflow_json_input#-1962547220> <targetProp1> "val 1" .
<urn:instance:variable_workflow_json_input#-1962547220> <targetProp2> "val 2" ."""

  final val variableWorkflowResponseCsvExample =
    """targetProp1,targetProp2
val 1,val 2"""

  final val workflowInfoListExample =
    """
      [
        {
          "id": "0c338c22-c43e-4a1c-960d-da44b8176c56_Workflowmultipleofthesameinputandoutput",
          "label": "Workflow multiple of the same input and output",
          "projectId": "singleProject",
          "projectLabel": "Simple variable workflow project",
          "variableInputs": [
            "1e80c0ed-9ca9-4d67-8868-65f7655aa416_Variableinputdataset"
          ],
          "variableOutputs": [
            "3a41ee9d-1ee7-4abe-9a62-603015abdb20_VariableOutput"
          ]
        },
        {
          "id": "67fe02eb-43a7-4b74-a6a2-c65a5c097636_Workflowoutputonly",
          "label": "Workflow output only",
          "projectId": "singleProject",
          "projectLabel": "Simple variable workflow project",
          "variableInputs": [],
          "variableOutputs": [
            "3a41ee9d-1ee7-4abe-9a62-603015abdb20_VariableOutput"
          ]
        }
      ]
    """

  final val workflowInfoExample =
    """
      {
        "id": "0c338c22-c43e-4a1c-960d-da44b8176c56_Workflowmultipleofthesameinputandoutput",
        "label": "Workflow multiple of the same input and output",
        "projectId": "singleProject",
        "projectLabel": "Simple variable workflow project",
        "variableInputs": [
          "1e80c0ed-9ca9-4d67-8868-65f7655aa416_Variableinputdataset"
        ],
        "variableOutputs": [
          "3a41ee9d-1ee7-4abe-9a62-603015abdb20_VariableOutput"
        ]
      }
    """

  final val workflowExecutionVariablesExample =
    """
      {
        "variables": [
          {
            "name": "baseUrl",
            "required": false,
            "default": {
              "name": "baseUrl",
              "value": "https://example.org",
              "description": "Base URL of the service",
              "isSensitive": false,
              "scope": "execution"
            },
            "referencedBy": [
              { "id": "fetchTemplate", "label": "Fetch", "taskType": "task" }
            ],
            "setBy": []
          },
          {
            "name": "greeting",
            "required": true,
            "referencedBy": [
              { "id": "greetingTemplate", "label": "Build greeting", "taskType": "task" }
            ],
            "setBy": []
          },
          {
            "name": "tmp",
            "required": false,
            "referencedBy": [
              { "id": "useTmp", "label": "Use tmp", "taskType": "task" }
            ],
            "setBy": [
              { "id": "setTmp", "label": "Set tmp", "taskType": "task" }
            ]
          }
        ]
      }
    """

  final val portsResponseDescription =
    """A workflow node port config can be configured on three different levels.
The most specific one is the config by node ID, i.e. of a specific node in the workflow graph.
The next level is the task level and contains the port config for a concrete project task that does not have
a port config that is determined by its item type.
The most general level is the item type level, where the item type itself defines the port config.
For a specific node in a workflow the most specific matching level should be taken, e.g. if a node has
a port config by node ID this should be used.
Each port config specifies the min. number of ports and an optional max. number of ports.
If the max. number is missing, this basically means that an arbitrary number of inputs/ports are allowed.
    """

  final val portsResponseExample =
    """
      {
        "byItemType": {
          "dataset": {
            "minInputPorts": 1
          },
          "linking": {
            "maxInputPorts": 2,
            "minInputPorts": 2
          },
          "transform": {
            "minInputPorts": 1
          },
          "workflow": {
            "minInputPorts": 1
          }
        },
        "byNodeId": {
          "node1": {
            "minInputPorts": 1,
            "maxInputPorts": 2
          }
        },
        "byTaskId": {
          "23586f0a-037d-4acd-91ad-669afe05a074_JSONparser": {
            "minInputPorts": 1
          },
          "fourPort": {
            "maxInputPorts": 4,
            "minInputPorts": 4
          },
          "noSchema": {
            "minInputPorts": 1
          },
          "onePort": {
            "maxInputPorts": 1,
            "minInputPorts": 1
          }
        }
      }
    """

}
