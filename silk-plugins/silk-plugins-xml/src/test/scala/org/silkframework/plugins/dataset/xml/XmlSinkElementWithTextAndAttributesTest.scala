package org.silkframework.plugins.dataset.xml

import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.rule.TransformSpec
import org.silkframework.rule.execution.ExecuteTransform
import org.silkframework.runtime.activity.UserContext
import org.silkframework.workspace.SingleProjectWorkspaceProviderTestTrait

import scala.xml.{Elem, Utility, XML}

/**
  * Tests a special feature of the XML dataset, i.e. using #text paths as target property of a mapping, which adds the
  * value to the text node of the parent element.
  */
class XmlSinkElementWithTextAndAttributesTest extends FlatSpec with MustMatchers with SingleProjectWorkspaceProviderTestTrait {
  behavior of "XML Sink #text feature"
  private final val TRANSFORM = "personTransform"
  private final val OUTPUT_RESOURCE = "person.xml"

  override def workspaceProviderName: String = "inMemory"

  private val expectedOutput: Elem = {
    <Persons>
      <Person age="23">John Doe</Person>
      <Person age="55">Max Power</Person>
    </Persons>
  }

  it should "run the workflow and generate the correct result" in {
    project.task[TransformSpec](TRANSFORM).activity[ExecuteTransform].control.startBlocking()
    val result = project.resources.get(OUTPUT_RESOURCE).loadAsString
    Utility.trim(XML.loadString(result)) mustBe Utility.trim(expectedOutput)
  }

  override def projectPathInClasspath: String = "org/silkframework/plugins/dataset/xml/xmlElementTextPlusAttr.zip"

  override def projectId: String = "testProject"
}
