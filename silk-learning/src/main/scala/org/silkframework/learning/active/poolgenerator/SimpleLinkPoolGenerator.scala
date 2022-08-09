/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.silkframework.learning.active.poolgenerator

import org.silkframework.config.{PlainTask, Prefixes}
import org.silkframework.dataset.DataSource
import org.silkframework.entity._
import org.silkframework.learning.active.comparisons.ComparisonPair
import org.silkframework.learning.active.poolgenerator.LinkPoolGeneratorUtils._
import org.silkframework.learning.active.LinkCandidate
import org.silkframework.rule.execution.{GenerateLinks, Linking}
import org.silkframework.rule.input.PathInput
import org.silkframework.rule.plugins.distance.equality.EqualityMetric
import org.silkframework.rule.plugins.transformer.normalize.TrimTransformer
import org.silkframework.rule.similarity.SimilarityOperator
import org.silkframework.rule.{LinkSpec, LinkageRule, Operator, RuntimeLinkingConfig}
import org.silkframework.runtime.activity.Status.Canceling
import org.silkframework.runtime.activity.{Activity, ActivityContext, ActivityControl, UserContext}
import org.silkframework.util.{DPair, Identifier}

import scala.util.Random

case class SimpleLinkPoolGenerator() extends LinkPoolGenerator {

  override def generator(inputs: DPair[DataSource],
                         linkSpec: LinkSpec,
                         paths: Seq[ComparisonPair],
                         randomSeed: Long)
                        (implicit prefixes: Prefixes): Activity[UnlabeledLinkPool] = {
    new LinkPoolGenerator(inputs, linkSpec, paths)
  }

  def runtimeConfig = RuntimeLinkingConfig(partitionSize = 100, useFileCache = false, generateLinksWithEntities = true)

  private class LinkPoolGenerator(inputs: DPair[DataSource],
                                  linkSpec: LinkSpec,
                                  paths: Seq[ComparisonPair])
                                 (implicit prefixes: Prefixes) extends Activity[UnlabeledLinkPool] {

    override val initialValue = Some(UnlabeledLinkPool.empty)

    private val maxLinks = 1000

    private var generateLinksActivity: ActivityControl[Linking] = _

    override def run(context: ActivityContext[UnlabeledLinkPool])
                    (implicit userContext: UserContext): Unit = {
      val entitySchemata = entitySchema(linkSpec, paths)
      val op = new SampleOperator()
      val linkSpec2 = linkSpec.copy(rule = LinkageRule(op))

      val generateLinks = new GenerateLinks(PlainTask("PoolGenerator", linkSpec2), inputs, None, runtimeConfig) {
         override def entityDescs = entitySchemata
      }

      generateLinksActivity = context.child(generateLinks, 0.8)

      val listener = (v: Linking) => {
        if (v.links.size > maxLinks) generateLinksActivity.cancel()
      }
      context.status.updateProgress(0.0)

      generateLinksActivity.value.subscribe(listener)
      generateLinksActivity.startBlocking()

      val generatedLinks = op.getLinks()
      assert(generatedLinks.nonEmpty || context.status().isInstanceOf[Canceling], "The unlabeled pool generator could not find any link candidates")

      if(generatedLinks.nonEmpty) {
        context.value.update(UnlabeledLinkPool(entitySchemata, shuffleLinks(generatedLinks)))
      }
    }

    private class SampleOperator(implicit userContext: UserContext) extends SimilarityOperator {

      val links = Array.fill(paths.size)(Seq[Link]())

      def getLinks() = {
        val a = links.flatten.distinct
        //val c = a.groupBy(_.source).values.map(randomElement(_))
        //         .groupBy(_.target).values.map(randomElement(_))
        Random.shuffle(a.toSeq).take(maxLinks).asInstanceOf[Seq[LinkCandidate]]
      }

      val metric = EqualityMetric()

      val transforms = Seq(TrimTransformer())

      val maxDistance = 0.0

      /** Maximum number of indices per property. If a property has more indices the remaining indices are ignored. */
      val maxIndices = 5

      def apply(entities: DPair[Entity], limit: Double = 0.0): Option[Double] = {
        for ((ComparisonPair(sourcePath, targetPath, _, _), index) <- paths.zipWithIndex) {
          var sourceValues = entities.source.evaluate(sourcePath)
          var targetValues = entities.target.evaluate(targetPath)
          for(transform <- transforms) {
            sourceValues = transform(Seq(sourceValues))
            targetValues = transform(Seq(targetValues))
          }
          val size = links(index).size

          if (size <= maxLinks && metric(sourceValues, targetValues, maxDistance) <= maxDistance) {
            links(index) :+= new LinkCandidate(entities.source, entities.target)
          }

          if (size > maxLinks)
            generateLinksActivity.cancel()
        }

        None
      }

      val id = Identifier.random

      val weight = 1

      val indexing = true

      private val sourceInputs = paths.map(_.source).distinct.map(p => PathInput(path = p.toUntypedPath))

      private val targetInputs = paths.map(_.target).distinct.map(p => PathInput(path = p.toUntypedPath))

      def index(entity: Entity, sourceOrTarget: Boolean, limit: Double): Index = {
        val inputs = if(sourceOrTarget) sourceInputs else targetInputs

        var inputValues = inputs.map(i => i(entity))
        for(transform <- transforms) {
          inputValues = inputValues.map(values => transform(Seq(values)))
        }

        val index = inputValues.map(metric.index(_, maxDistance, sourceOrTarget).crop(maxIndices)).reduce(_ merge _)

        index
      }

      def children = Seq.empty

      def withChildren(newChildren: Seq[Operator]): Operator = ???
    }
  }
}