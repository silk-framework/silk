package org.silkframework.rule

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.rule.input.{PathInput, RuleBlockBinding, RuleBlockInput}
import org.silkframework.rule.plugins.distance.equality.EqualityMetric
import org.silkframework.rule.similarity.Comparison
import org.silkframework.util.DPair

class RuleBlockTaskReferenceTest extends AnyFlatSpec with Matchers {

  behavior of "Rule block task references"

  it should "expose referenced rule block tasks from transform specs" in {
    val ruleBlockId = "normalizeName"
    val transformSpec = TransformSpec(
      selection = DatasetSelection("inputDataset"),
      mappingRule = RootMappingRule(
        MappingRules(
          uriRule = Some(
            ComplexUriMapping(
              operator = RuleBlockInput(
                id = "ruleBlockUsage",
                ruleBlockId = ruleBlockId,
                bindings = IndexedSeq(
                  RuleBlockBinding(
                    portId = "nameInput",
                    input = PathInput(id = "namePath", path = UntypedPath("name"))
                  )
                )
              )
            )
          )
        )
      )
    )

    transformSpec.referencedTasks must contain(ruleBlockId)
  }

  it should "expose referenced rule block tasks from link specs" in {
    val ruleBlockId = "normalizeName"
    val linkSpec = LinkSpec(
      source = DatasetSelection("sourceDataset"),
      target = DatasetSelection("targetDataset"),
      rule = LinkageRule(
        Some(
          Comparison(
            id = "comparison",
            metric = EqualityMetric(),
            inputs = DPair(
              RuleBlockInput(
                id = "ruleBlockUsage",
                ruleBlockId = ruleBlockId,
                bindings = IndexedSeq(
                  RuleBlockBinding(
                    portId = "nameInput",
                    input = PathInput(id = "sourceName", path = UntypedPath("name"))
                  )
                )
              ),
              PathInput(id = "targetName", path = UntypedPath("name"))
            )
          )
        )
      )
    )

    linkSpec.referencedTasks must contain(ruleBlockId)
  }
}
