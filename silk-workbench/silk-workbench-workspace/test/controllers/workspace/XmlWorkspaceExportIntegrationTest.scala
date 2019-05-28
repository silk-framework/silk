package controllers.workspace

import org.scalatest.FlatSpec

/**
  * Tests the workspace and project export features.
  */
class XmlWorkspaceExportIntegrationTest extends FlatSpec with ExportIntegrationTestTrait {
  behavior of "XML workspace export"

  it should "export project with broken workspace provider" in {
    makeWorkspaceFail()
    val projectExportUri = baseUrl + s"/workspace/projects/$projectId/export/xmlZip"
    val responseBody = checkResponse(client.url(projectExportUri).get()).bodyAsBytes.toArray
    checkZipEntries(responseBody, Seq(
      "singleProject/workflow/Workflow.xml",
      "singleProject/linking/miniLinking/alignment.xml",
      "singleProject/linking/miniLinking/linkSpec.xml",
      "singleProject/transform/miniTransform/rules.xml",
      "singleProject/transform/miniTransform/dataset.xml",
      "singleProject/dataset/miniCsv.xml",
      "singleProject/dataset/internalDataset.xml",
      "singleProject/resources/miniResource.csv",
      "singleProject/config.xml"))
  }

  it should "export workspace with broken workspace provider" in {
    makeWorkspaceFail()
    val workspaceExportURI = baseUrl + "/workspace/export/xmlZip"
    val responseBody = checkResponse(client.url(workspaceExportURI).get()).bodyAsBytes.toArray
    checkZipEntries(responseBody, Seq(
      "singleProject/workflow/Workflow.xml",
      "singleProject/linking/miniLinking/alignment.xml",
      "singleProject/linking/miniLinking/linkSpec.xml",
      "singleProject/transform/miniTransform/rules.xml",
      "singleProject/transform/miniTransform/dataset.xml",
      "singleProject/dataset/miniCsv.xml",
      "singleProject/dataset/internalDataset.xml",
      "singleProject/resources/miniResource.csv",
      "singleProject/config.xml"
    ))
  }
}