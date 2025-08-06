package org.silkframework.plugins.dataset.xml


import org.silkframework.runtime.activity.UserContext
import org.silkframework.workspace.SingleProjectWorkspaceProviderTestTrait

import scala.xml.{Elem, Utility, XML}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class XSLTOperatorTest extends AnyFlatSpec with Matchers with SingleProjectWorkspaceProviderTestTrait {
  private final val WORKFLOW = "xsltWorkflow"
  private final val OUTPUT_RESOURCE = "output.xml"

  override def workspaceProviderId: String = "inMemoryWorkspaceProvider"

  private val expectedOutput: Elem = {
    <GroupedPerson>
      <Group>
        <Key>1</Key>
        <Members>Max Doe</Members>
      </Group>
      <Group>
        <Key>2</Key>
        <Members>Max Noe, Max Poe</Members>
      </Group>
    </GroupedPerson>
  }

  it should "run the XSLT workflow and generate the correct result" in {
    implicit val userContext: UserContext = UserContext.Empty
    executeWorkflow(WORKFLOW)
    val result = project.resources.get(OUTPUT_RESOURCE).loadAsString()
    Utility.trim(XML.loadString(result)) mustBe Utility.trim(expectedOutput)
  }

  override def projectPathInClasspath = "org/silkframework/plugins/dataset/xml/xsltProject.zip"

  override def projectId = "xsltProject"
}
