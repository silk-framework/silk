package controllers.workspace.doc

object LegacyDatasetApiDoc {

  final val datasetExampleJson =
    """
      {
        "id": "DatasetName",
        "data": {
          "type": "file",
          "parameters": {
            "file": "dataset.nt",
            "format": "N-TRIPLE"
          }
        }
      }
    """

  final val datasetExampleXml =
    """
      <Dataset id="DatasetName" type="file">
        <Param name="file" value="dataset.nt"/>
        <Param name="format" value="N-TRIPLE"/>
      </Dataset>
    """

  final val mappingCoverageDescription =
    """Returns the mapping coverage of a this dataset. The mapping coverage is derived from all transformations from the same project
that have this dataset as input. It has three categories, fully mapped, partially mapped and unmapped. A source path is fully mapped
if it only consists of forward paths and no backward paths or filters. If there are filters or backward paths then it can maximally be
considered as partially mapped, although in reality several partial mappings could fully cover a path. The algorithm cannot detect such
kind of combined coverage. A path is unmapped if it is not uses as value input in any mapping.
    """

  final val mappingCoverageExampleResponse =
    """
      [
        {
          "covered": true,
          "fully": true,
          "path": "target:label"
        },
        {
          "covered": true,
          "fully": false,
          "path": "target:zipCodeArea"
        },
        {
          "covered": false,
          "fully": false,
          "path": "target:issueDate"
        }
      ]
    """

  final val mappingValueCoverageDescription =
    """Returns mapping value coverage details for a specific dataset path. This is mostly relevant for partially mapped paths.
It takes a specific path as input and returns the number of values found at this path, the number of values actually
used by all mapping of the project and values that are not covered.
    """

  final val mappingValueCoverageExampleResponse =
    """
      {
        "coveredValues": 2,
        "missedValues": [
          {
            "nodeId": "953217152",
            "value": "V2"
          }
        ],
        "overallValues": 3
      }
    """

}
