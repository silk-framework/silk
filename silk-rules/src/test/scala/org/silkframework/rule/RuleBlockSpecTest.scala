package org.silkframework.rule

import org.scalatest.flatspec.AnyFlatSpec
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.rule.input.{PathInput, TransformInput}
import org.silkframework.rule.plugins.transformer.value.ConstantTransformer
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.validation.ValidationException
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
            exampleValues = "---\n- value 1\n- |\n  multi\n  line\n",
            displayOrder = 0
          ),
          RuleBlockPort(
            id = "secondInput",
            label = "Second input",
            description = "Deprecated fallback.",
            exampleValues = "---\n- fallback\n",
            displayOrder = 1,
            deprecated = true
          )
        ),
        operator = Some(TransformInput(id = "rootTransform", transformer = ConstantTransformer("constant result"))),
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
            RuleBlockPort(id = "duplicate"),
            RuleBlockPort(id = "duplicate")
          )
        )
      )
    }

    ex.getMessage must include ("Duplicate rule block port IDs")
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
