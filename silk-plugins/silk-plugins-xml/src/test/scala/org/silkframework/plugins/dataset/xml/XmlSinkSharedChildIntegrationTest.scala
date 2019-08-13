package org.silkframework.plugins.dataset.xml

import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.rule.TransformSpec
import org.silkframework.rule.execution.ExecuteTransform
import org.silkframework.runtime.activity.UserContext
import org.silkframework.workspace.SingleProjectWorkspaceProviderTestTrait

import scala.xml.{Utility, XML}

class XmlSinkSharedChildIntegrationTest extends FlatSpec with MustMatchers with SingleProjectWorkspaceProviderTestTrait {
  behavior of "XML Sink"

  override def projectPathInClasspath = "org/silkframework/plugins/dataset/xml/cmem-1023-xmlSharedChild.zip"

  override def projectId = "xmlSharedChildBug"

  override def workspaceProviderName: String = "inMemory"

  private val TRANSFORM_TASK = "transformRDF2XML"
  private val OUTPUT_XML_RESOURCE = "output.xml"

  private val expectedXML = {
    <Root>
      <Child>
        <child>
          <shared>
            <label>non shared 1</label>
          </shared>
          <shared>
            <label>shared</label>
          </shared>
        </child>
        <child>
          <shared>
            <label>non shared 2</label>
          </shared>
          <shared>
            <label>shared</label>
          </shared>
        </child>
      </Child>
    </Root>
  }

  it should "Attach shared source child resources to both target elements" in {
    project.task[TransformSpec](TRANSFORM_TASK).activity[ExecuteTransform].control.startBlocking()
    val xml = project.resources.get(OUTPUT_XML_RESOURCE).loadAsString
    Utility.trim(XML.loadString(xml)) mustBe Utility.trim(expectedXML)
  }
}
