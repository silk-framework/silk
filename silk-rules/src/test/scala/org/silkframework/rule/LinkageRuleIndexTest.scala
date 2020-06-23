package org.silkframework.rule

import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.entity.{Entity, EntitySchema}
import org.silkframework.rule.input.PathInput
import org.silkframework.rule.plugins.distance.equality.EqualityMetric
import org.silkframework.rule.similarity.Comparison
import org.silkframework.util.{DPair, Identifier}

class LinkageRuleIndexTest extends FlatSpec with MustMatchers {

  behavior of "Linkage rule index"

  private val sourcePath = "pathSource"
  private val targetPath = "pathTarget"
  private val inputs = DPair(PathInput(path = UntypedPath(sourcePath)), PathInput(path = UntypedPath(targetPath)))
  private val comparison = Comparison(metric = EqualityMetric(), inputs = inputs)
  private val linkingRule = new LinkageRule(Some(comparison))
  private val sourceEntitySchema = entitySchema(Seq(sourcePath))
  private val targetEntitySchema = entitySchema(Seq(targetPath))

  private def entitySchema(paths: Seq[String],
                           typeUri: Identifier = Identifier.random): EntitySchema = {
    EntitySchema(typeUri.toString, paths.map(p => UntypedPath(p).asStringTypedPath).toIndexedSeq)
  }

  private def entity(entitySchema: EntitySchema, values: Seq[String]): Entity = {
    Entity(Identifier.random.toString, IndexedSeq(values), entitySchema)
  }

  it should "create a linkage rule tree for a simple comparison" in {
    val sourceEntity = entity(sourceEntitySchema, Seq("A3434657286572348", "89gf8sdzgf89sd"))
    val targetEntity = entity(targetEntitySchema, Seq("dfgfsdgfsdgfgfsjhhthrthtterhtehethetrd", "2", "3"))
    val sourceIndex = LinkageRuleIndex(sourceEntity, linkingRule, sourceOrTarget = true)
    val targetIndex = LinkageRuleIndex(targetEntity, linkingRule, sourceOrTarget = false)
    sourceIndex mustBe LinkageRuleIndex(
      LinkageRuleIndexComparison(comparison.id, LinkageRuleIndexInput(inputs.source.id, Set(1088895554, 2034273890))))
    targetIndex mustBe LinkageRuleIndex(
      LinkageRuleIndexComparison(comparison.id, LinkageRuleIndexInput(inputs.target.id, Set(-83525085, 50, 51))))
  }
}
