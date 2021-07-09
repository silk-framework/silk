package controllers.workflowApi.doc

object WorkflowApiDoc {

  final val variableWorkflowResultPostDescription =
    """Executes a workflow with parameters from the request query string or form URL encoded body.
Compatible with all workflows that contain a single variable dataset that is used as input dataset and a single
variable dataset as output – each dataset can be used several times in the same workflow.
The output data type is specified via the ACCEPT header. The result is returned as the requested mime type,
the content is the file content of the corresponding dataset, e.g. XML, CSV etc.
A single entity is build from the provided request parameters and injected into the variable source dataset.
There must be at least one form or query parameter specified in the request. If empty entities as input
must be supported, a POST request with empty JSON or XML object/element should be used, see below.
For some data types (JSON, XML and CSV), the POST body can contain arbitrary content that the data source
is expected to have. This goes beyond the simple query or form parameter input, where only exactly one input entity would be generated.
The corresponding content type must be specified in these cases."""

  final val variableWorkflowResultGetDescription =
    """For a GET request the parameter and values are provided via the query string of the URL.
In order to provide multiple values for an input property, the same query parameter should be used multiple times,
e.g. `inputProp=value+1&inputProp=value+2`.
The responses are the same as for the POST request."""

  final val variableWorkflowRequestFormsExample = "param1=param1+value&param2=param2+value&param2=param2+second+value"

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

}
