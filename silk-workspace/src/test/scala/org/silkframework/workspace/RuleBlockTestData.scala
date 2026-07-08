package org.silkframework.workspace

import org.silkframework.config.{MetaData, PlainTask}
import org.silkframework.rule.input.{InputPortInput, TransformInput}
import org.silkframework.rule.plugins.transformer.combine.ConcatTransformer
import org.silkframework.rule.{NodePosition, RuleBlockInputExample, RuleBlockModel, RuleBlockPort, RuleBlockSpec, RuleLayout}
import org.silkframework.util.Identifier
import org.silkframework.workspace.annotation.{StickyNote, UiAnnotations}

object RuleBlockTestData {
  def sampleRuleBlockSpec(): RuleBlockSpec = {
    val namePortId = Identifier("namePort")
    RuleBlockSpec(
      RuleBlockModel(
        ports = IndexedSeq(
          RuleBlockPort(
            id = namePortId,
            label = "Name",
            description = "Normalized name values",
            displayOrder = 1
          )
        ),
        inputExamples = IndexedSeq(
          RuleBlockInputExample(
            id = Identifier("example-1"),
            label = Some("Example input"),
            inputs = Map(namePortId -> Seq("Alice"))
          )
        ),
        operator = Some(
          TransformInput(
            id = Identifier("concatRuleBlock"),
            transformer = ConcatTransformer(glue = " "),
            inputs = IndexedSeq(
              InputPortInput(id = Identifier("nameInputPortNode"), portId = namePortId)
            )
          )
        ),
        layout = RuleLayout(
          nodePositions = Map(
            "concatRuleBlock" -> NodePosition(1, 2),
            "nameInputPortNode" -> NodePosition(3, 4, 120, 80)
          )
        ),
        uiAnnotations = UiAnnotations(
          stickyNotes = Seq(
            StickyNote("ruleBlockSticky", "Rule block note", "#abc", NodePosition(5, 6, 140, 90))
          )
        )
      )
    )
  }

  def sampleRuleBlockTask(id: Identifier = Identifier("normalizeName"),
                          metaData: MetaData = MetaData.empty): PlainTask[RuleBlockSpec] = {
    PlainTask(id, sampleRuleBlockSpec(), metaData)
  }
}
