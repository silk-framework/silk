package org.silkframework.rule

import org.scalatest.flatspec.AnyFlatSpec
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.rule.input.{InputPortInput, PathInput, TransformInput}
import org.silkframework.rule.plugins.transformer.combine.ConcatTransformer
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.serialization.XmlSerialization.fromXml
import org.silkframework.runtime.serialization.{ReadContext, TestReadContext}
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.Identifier
import org.silkframework.util.XmlSerializationHelperTrait
import org.silkframework.workspace.annotation.{StickyNote, UiAnnotations}

class RuleBlockSpecTest extends AnyFlatSpec with XmlSerializationHelperTrait {

  behavior of "RuleBlockSpec"

  it should "serialize and deserialize rule block specs and tasks" in {
    val spec = RuleBlockSpec(
      RuleBlockModel(
        ports = IndexedSeq(
          RuleBlockPort(
            id = "firstInput",
            label = "First input",
            description = "Used for the primary lookup.",
            displayOrder = 1
          ),
          RuleBlockPort(
            id = "secondInput",
            label = "Second input",
            description = "Deprecated fallback.",
            displayOrder = 2,
            deprecated = true
          )
        ),
        inputExamples = IndexedSeq(
          RuleBlockInputExample(
            id = "example-1",
            label = Some("Primary example"),
            inputs = Map(
              Identifier("firstInput") -> Seq("value 1", "multi\nline"),
              Identifier("secondInput") -> Seq("fallback")
            )
          )
        ),
        operator = Some(
          TransformInput(
            id = "rootTransform",
            transformer = ConcatTransformer(glue = " "),
            inputs = IndexedSeq(
              InputPortInput(id = "firstInputNode", portId = "firstInput"),
              InputPortInput(id = "secondInputNode", portId = "secondInput")
            )
          )
        ),
        layout = RuleLayout(Map("rootTransform" -> NodePosition(10, 20, Some(120), Some(60)))),
        uiAnnotations = UiAnnotations(Seq(StickyNote("note", "review this", "#fff", NodePosition(5, 7, Some(100), Some(40)))))
      )
    )

    testRoundTripSerialization(spec)
    testRoundTripSerialization(RuleBlockTask("ruleBlock", spec))

    implicit val pluginContext: PluginContext = PluginContext.empty
    spec.parameters.values.keySet must contain only "ruleBlockModel"
  }

  it should "reject duplicate port identifiers" in {
    val ex = the[ValidationException] thrownBy {
      RuleBlockSpec(
        RuleBlockModel(
          ports = IndexedSeq(
            RuleBlockPort(id = "duplicate", label = "Duplicate input A"),
            RuleBlockPort(id = "duplicate", label = "Duplicate input B")
          )
        )
      )
    }

    ex.getMessage must include ("Duplicate rule block port IDs")
  }

  it should "reject duplicate display orders" in {
    val ex = the[ValidationException] thrownBy {
      RuleBlockSpec(
        RuleBlockModel(
          ports = IndexedSeq(
            RuleBlockPort(id = "first", label = "First input", displayOrder = 1),
            RuleBlockPort(id = "second", label = "Second input", displayOrder = 1)
          )
        )
      )
    }

    ex.getMessage must include ("Duplicate rule block port display orders")
  }

  it should "reject duplicate input example identifiers" in {
    val ex = the[ValidationException] thrownBy {
      RuleBlockSpec(
        RuleBlockModel(
          inputExamples = IndexedSeq(
            RuleBlockInputExample(id = "duplicate"),
            RuleBlockInputExample(id = "duplicate")
          )
        )
      )
    }

    ex.getMessage must include ("Duplicate rule block input example IDs")
  }

  it should "default missing port display orders in XML to one" in {
    implicit val readContext: ReadContext = TestReadContext()
    val spec = fromXml[RuleBlockSpec] {
      <RuleBlock>
        <RuleBlockModel>
          <Ports>
            <Port id="firstInput" label="First input" deprecated="false">
              <Description>Used for the primary lookup.</Description>
            </Port>
          </Ports>
        </RuleBlockModel>
      </RuleBlock>
    }

    spec.ports.map(_.displayOrder) mustBe IndexedSeq(1)
  }

  it should "reject input examples that reference unknown ports" in {
    val ex = the[ValidationException] thrownBy {
      RuleBlockSpec(
        RuleBlockModel(
          ports = IndexedSeq(
            RuleBlockPort(id = "knownPort", label = "Known input")
          ),
          inputExamples = IndexedSeq(
            RuleBlockInputExample(
              id = "example-1",
              inputs = Map(Identifier("unknownPort") -> Seq("value"))
            )
          )
        )
      )
    }

    ex.getMessage must include ("references unknown input port")
  }

  it should "reject path inputs in the internal operator tree" in {
    val ex = the[ValidationException] thrownBy {
      RuleBlockSpec(
        RuleBlockModel(
          operator = Some(PathInput(id = "pathInput", path = UntypedPath("foaf:name")))
        )
      )
    }

    ex.getMessage must include ("must not contain path inputs")
  }
}
