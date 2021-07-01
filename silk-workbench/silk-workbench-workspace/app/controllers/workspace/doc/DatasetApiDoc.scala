package controllers.workspace.doc

object DatasetApiDoc {

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

}
