package org.silkframework.rule

import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.entity.Path
import org.silkframework.rule.input.{PathInput, TransformInput}
import org.silkframework.rule.plugins.transformer.normalize.LowerCaseTransformer
import org.silkframework.rule.plugins.transformer.value.ConstantUriTransformer
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.Uri

class TransformRuleTest extends FlatSpec with MustMatchers {
  behavior of "Transform Rule"

  val duplicated1 = "duplicated1"
  val duplicated2 = "duplicated2"

  it should "validate IDs in nested rules" in {
    createRootRule(duplicate1 = false, duplicate2 = false)
    testErrorCases(duplicate1 = true, duplicate2 = true)
    testErrorCases(duplicate1 = false, duplicate2 = true)
    testErrorCases(duplicate1 = true, duplicate2 = false)
  }

  it should "validate IDs in rule operator trees" in {
    intercept[ValidationException] {
      createRuleWithDuplicatedOperatorID()
    }
  }

  private def testErrorCases(duplicate1: Boolean, duplicate2: Boolean): Unit = {
    intercept[ValidationException]{
      createRootRule(duplicate1 = duplicate1, duplicate2 = duplicate2)
    }
    try {
      createRootRule(duplicate1 = duplicate1, duplicate2 = duplicate2)
    } catch {
      case e: ValidationException =>
        val errorMessage = e.errors.map(_.message).mkString("")
        if(duplicate1) {
          assert(errorMessage.contains(duplicated1))
        } else {
          assert(!errorMessage.contains(duplicated1))
        }
        if(duplicate2) {
          assert(errorMessage.contains(duplicated2))
        } else {
          assert(!errorMessage.contains(duplicated2))
        }
    }
  }

  private def createRootRule(duplicate1: Boolean, duplicate2: Boolean) = {
    val rootMapping = RootMappingRule(
      MappingRules(
        uriRule = Some(ComplexUriMapping(duplicated1, operator = TransformInput(transformer = ConstantUriTransformer()))),
        typeRules = Seq(TypeMapping(duplicated2)),
        propertyRules = Seq(
          ObjectMapping(
            "object",
            rules = MappingRules(
              uriRule = Some(ComplexUriMapping("uriRuleNested", operator = TransformInput(transformer = ConstantUriTransformer()))),
              typeRules = Seq(TypeMapping("typeRuleNested")),
              propertyRules = Seq(
                DirectMapping(if (duplicate2) duplicated2 else "somethingElse1"),
                ComplexMapping(if (duplicate1) duplicated1 else "somethingElse2", operator = TransformInput(transformer = ConstantUriTransformer()))
              )
            )
          )
        )
      )
    )
    rootMapping
  }

  private def createRuleWithDuplicatedOperatorID() = {
    val rootMapping = RootMappingRule(
      MappingRules(
        uriRule = None,
        typeRules = Seq.empty,
        propertyRules = Seq(
          ComplexMapping("invalidRule", TransformInput("duplicateID", LowerCaseTransformer(), Seq(PathInput("duplicateID", Path.empty))))
        )
      )
    )
    rootMapping
  }

  private def checkTargetUri(targetPropertyUri: String) = {
    // Nest it, so we know it works recursively
    val objectMapping = ObjectMapping(rules = MappingRules(propertyRules = Seq(
      DirectMapping(mappingTarget = MappingTarget(Uri(targetPropertyUri)))
    )))
    RootMappingRule(MappingRules(propertyRules = Seq(objectMapping)))
  }
}
