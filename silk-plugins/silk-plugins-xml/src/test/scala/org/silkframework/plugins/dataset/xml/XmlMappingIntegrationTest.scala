package org.silkframework.plugins.dataset.xml


import org.silkframework.rule.TransformSpec
import org.silkframework.workspace.SingleProjectWorkspaceProviderTestTrait
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

case class XmlMappingIntegrationTest() extends AnyFlatSpec with SingleProjectWorkspaceProviderTestTrait with Matchers {
  behavior of "XML mapping integration test"

  override def projectPathInClasspath: String = "org/silkframework/plugins/dataset/xml/0a7d0894-5ece-4f4a-8ee5-d6faf483b242_XMLmappingtestproject.zip"

  override def workspaceProviderId: String = "inMemoryWorkspaceProvider"

  private val transformId = "f5f24c45-1d0f-45e8-99ce-12fb5c0dbd95_TransformXML"

  it should "generate default URIs for attributes as object and allow access to attribute value via #text path" in {
    // The project archive ships an output.nt holding a triple of a previous run. Emptying it first makes this test
    // independent of when that resource is materialized during the project import.
    project.resources.get("output.nt").writeString("")
    project.task[TransformSpec](transformId).activity("ExecuteTransform").control.startBlocking()
    val nTriplesContent = project.resources.get("output.nt").loadLines()
    withClue(s"generated triples:\n${nTriplesContent.mkString("\n")}\n") {
      nTriplesContent.size mustBe 3
    }
    nTriplesContent.head must (include ("attr_count") and include ("attribute"))
    nTriplesContent(1) must (include ("attr_count") and include ("attribute") and include ("<http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <urn:type:Type>"))
    nTriplesContent(2) must (include ("attr_count") and include ("attribute") and include ("<http://www.w3.org/2000/01/rdf-schema#label> \"2\""))
  }
}
