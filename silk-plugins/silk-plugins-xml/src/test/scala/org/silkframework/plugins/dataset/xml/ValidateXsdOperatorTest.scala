package org.silkframework.plugins.dataset.xml

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.config.PlainTask
import org.silkframework.dataset.LocalDatasetResourceEntityTable
import org.silkframework.entity.Entity
import org.silkframework.execution.ExecutorOutput
import org.silkframework.execution.local.LocalExecution
import org.silkframework.runtime.activity.ActivityMonitor
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.resource.InMemoryResourceManager

import scala.xml.Elem

class ValidateXsdOperatorTest extends AnyFlatSpec with Matchers {

  private val xsd: Elem = {
    <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">

      <xs:element name="name" type="xs:string"/>
      <xs:element name="age" type="xs:positiveInteger"/>
      <xs:element name="height" type="xs:positiveInteger"/>

      <xs:element name="Person">
        <xs:complexType>
          <xs:sequence>
            <xs:element ref="name"/>
            <xs:element ref="age" minOccurs="0"/>
            <xs:element ref="height" minOccurs="0"/>
          </xs:sequence>
        </xs:complexType>
      </xs:element>

    </xs:schema>
  }

  private val validXml: Elem = {
    <Person>
      <name>John Doe</name>
    </Person>
  }

  private val invalidXml: Elem = {
    <Person>
      <name>John Doe</name>
      <age>-1</age>
      <height>-100</height>
    </Person>
  }

  it should "return an empty result for valid XML files" in {
    execute(validXml, xsd) should have size 0
  }

  it should "return a list of errors for invalid XMl files" in {
    val result = execute(invalidXml, xsd)
    // We expect two errors, but the validator might return multiple entries for each
    result.size should be >= 2
    // Make sure line and column indices are returned
    result.head.values.slice(2, 4) shouldBe Seq(Seq("3"), Seq("20"))
  }

  private def execute(xml: Elem, xsd: Elem): IndexedSeq[Entity] = {
    // Create resources
    val resources = InMemoryResourceManager()
    val schemaResource = resources.get("schema.xsd")
    schemaResource.writeString(xsd.toString.trim)
    val xmlResource = resources.get("source.xml")
    xmlResource.writeString(xml.toString.trim)

    // Create operator
    val operator = ValidateXsdOperator(schemaResource)
    val task = PlainTask("test-operator", operator)
    val executor = LocalValidateXsdOperatorExecutor()
    implicit val pluginContext: PluginContext = PluginContext.empty

    // Execute
    val result = executor.execute(task, Seq(new LocalDatasetResourceEntityTable(xmlResource, task)), ExecutorOutput.empty, LocalExecution(), new ActivityMonitor("test"))
    result should not be None
    result.get.entities.toIndexedSeq
  }
}
