package controllers.workflow.doc

object WorkflowApiDoc {

  final val executeOnPayloadDescription =
    """Execute a variable workflow that gets the inputs for variable data sources with the HTTP request and
returns all results of variable data sinks with the HTTP response.
This endpoint will block until the workflow execution finished. Use executeOnPayloadAsynchronous for non-blocking execution."""

  final val executeOnPayloadAsynchronousDescription =
    """Execute a variable workflow that gets the inputs for variable data sources with the HTTP request and
returns all results of variable data sinks with the HTTP response.
This endpoint starts the workflow execution in the background and returns
the identifier of the started background activity.
Use the activity API to query for its exection status and result, e.g.,
`/workspace/projects/{projectId}/tasks/{taskId} /activities/{BackgroundActivityID}/{value or status}`.
After having consumed the result value a well-behaving client should remove the execution instance via
the `/execution/{executionId}` endpoint."""

  final val executeOnPayloadBodyDescription =
    """At the moment the file parameter follows the convention that the file name must be the dataset name
plus the string "_file_resource", e.g. dataset name "dOutput" with file parameter value "dOutput_file_resource".
This convention is not needed for data sources.
It is possible to reference project resource files. In order to use existing resources and not provide
them via the REST request, no resource element with the same name should be in the XML payload. Then,
if the project resource with the value given for the file parameter exists, this is used instead.
    """

  final val executeOnPayloadJsonRequestExample =
    """
      {
        "DataSources": [
          {
            "id": "inputDataset",
            "data": {
              "taskType": "Dataset",
              "type": "json",
              "parameters": {
                "file": "test_file_resource"
              }
            }
          }
        ],
        "Sinks": [
          {
            "id": "outputDataset",
            "data": {
              "taskType": "Dataset",
              "type": "file",
              "parameters": {
                "file": "outputResource",
                "format": "N-Triples"
              }
            }
          }
        ],
        "Resources": {
          "test_file_resource": [
            {"id":"1"},
            {"id":"2" }
          ]
        }
      }
    """

  final val executeOnPayloadXmlRequestExample =
    """
      <Workflow>
        <DataSources>
          <Dataset id="dSource">
            <DatasetPlugin type="file">
              <Param name="format" value="N-Triples"/>
              <Param name="file" value="dSource_file_resource"/>
            </DatasetPlugin>
          </Dataset>
          <Dataset id="dTarget">
            <DatasetPlugin type="file">
              <Param name="format" value="N-Triples"/>
              <Param name="file" value="dTarget_file_resource"/>
            </DatasetPlugin>
          </Dataset>
        </DataSources>
        <Sinks>
          <Dataset id="dOutput">
            <DatasetPlugin type="file">
              <Param name="format" value="N-Triples"/>
              <Param name="file" value="dOutput_file_resource"/>
            </DatasetPlugin>
          </Dataset>
        </Sinks>
        <resource name="dSource_file_resource">
          &lt;https://www.fuhsen.net/resource/xing/person/123456_abcdef&gt; &lt;http://www.w3.org/2000/01/rdf-schema#label&gt;
          &quot;Max Mustermann&quot; .
        </resource>
        <resource name="dTarget_file_resource">
          &lt;https://www.fuhsen.net/resource/xing/person/654321_abcdef&gt; &lt;http://www.w3.org/2000/01/rdf-schema#label&gt;
          &quot;mustermann, max&quot; .
        </resource>
      </Workflow>
    """

  final val executeOnPayloadJsonResponseExample =
    """
      [
        {
          "sinkId": "outputDataset",
          "textContent": "<urn:instance:test_file_resource#0> <test> \"1\"^^<http://www.w3.org/2001/XMLSchema#integer> .\n<urn:instance:test_file_resource#1> <test> \"2\"^^<http://www.w3.org/2001/XMLSchema#integer> ."
        }
      ]
    """

  final val executeOnPayloadXmlResponseExample =
    """
      <WorkflowResults>
        <Result sinkId="dOutput">
          &lt;https://www.fuhsen.net/resource/xing/person/123456_abcdef&gt; &lt;http://www.w3.org/2002/07/owl#sameAs&gt;
          &lt;https://www.fuhsen.net/resource/xing/person/654321_abcdef&gt; .
        </Result>
      </WorkflowResults>
    """

  final val removeVariableWorkflowExecutionDescription  =
    """
       Remove the workflow execution instance, which was started via the /executeOnPayloadAsynchronous endpoint.
       Since only a limited number of executions are kept at every moment,
       a well behaving client should remove the execution if the client has consumed the result.",
    """

}
