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

import org.silkframework.config.{LinkSpecification, RuntimeConfig}
import org.silkframework.dataset.DataSource
import org.silkframework.entity.{Entity, Index, Link, Path}
import org.silkframework.execution.{GenerateLinks, Linking}
import org.silkframework.learning.active.UnlabeledLinkPool
import org.silkframework.plugins.distance.equality.EqualityMetric
import org.silkframework.rule.input.PathInput
import org.silkframework.rule.similarity.SimilarityOperator
import org.silkframework.rule.{LinkageRule, Operator}
import org.silkframework.runtime.activity.{Activity, ActivityContext}
import org.silkframework.util.{DPair, Identifier}

import scala.util.Random

case class SimpleLinkPoolGenerator() extends LinkPoolGenerator {

  override def generator(inputs: DPair[DataSource],
                         linkSpec: LinkSpecification,
                         paths: DPair[Seq[Path]]): Activity[UnlabeledLinkPool] = {
    new LinkPoolGenerator(inputs, linkSpec, paths)
  }

  def runtimeConfig = RuntimeConfig(partitionSize = 100, useFileCache = false, generateLinksWithEntities = true)

  class LinkPoolGenerator(inputs: DPair[DataSource],
                          linkSpec: LinkSpecification,
                          paths: DPair[Seq[Path]]) extends Activity[UnlabeledLinkPool] {

    private val maxLinks = 1000

    private var generateLinksActivity: GenerateLinks = _

    override def run(context: ActivityContext[UnlabeledLinkPool]): Unit = {
      val entityDesc = DPair(linkSpec.entityDescriptions.source.copy(paths = paths.source.toIndexedSeq),
        linkSpec.entityDescriptions.target.copy(paths = paths.target.toIndexedSeq))
      val op = new SampleOperator()
      val linkSpec2 = linkSpec.copy(rule = LinkageRule(op))

      generateLinksActivity =
        new GenerateLinks(inputs, linkSpec2, Seq.empty, runtimeConfig) {
          override def entityDescs = entityDesc
        }

      val listener = (v: Linking) => {
        if (v.links.size > maxLinks) generateLinksActivity.cancelExecution()
      }
      context.status.update(0.0)
      context.executeBlocking(generateLinksActivity, 0.8, listener)

      val generatedLinks = op.getLinks()
      assert(generatedLinks.nonEmpty, "Could not load any links")

      val shuffledLinks = for ((s, t) <- generatedLinks zip (generatedLinks.tail :+ generatedLinks.head)) yield new Link(s.source, t.target, None, Some(DPair(s.entities.get.source, t.entities.get.target)))

      context.value.update(UnlabeledLinkPool(entityDesc, generatedLinks ++ shuffledLinks))
    }

    private class SampleOperator() extends SimilarityOperator {

      val links = Array.fill(paths.source.size, paths.target.size)(Seq[Link]())

      def getLinks() = {
        val a = links.flatten.flatten
        //val c = a.groupBy(_.source).values.map(randomElement(_))
        //         .groupBy(_.target).values.map(randomElement(_))
        Random.shuffle(a.toSeq).take(maxLinks)
      }

      val metric = EqualityMetric()

      val maxDistance = 0.0

      /** Maximum number of indices per property. If a property has more indices the remaining indices are ignored. */
      val maxIndices = 5

      def apply(entities: DPair[Entity], limit: Double = 0.0): Option[Double] = {
        for ((sourcePath, sourceIndex) <- paths.source.zipWithIndex;
             (targetPath, targetIndex) <- paths.target.zipWithIndex) {
          val sourceValues = entities.source.evaluate(sourcePath)
          val targetValues = entities.target.evaluate(targetPath)
          val size = links(sourceIndex)(targetIndex).size
          val labelLinks = links(0)(0).size

          if (size <= maxLinks && metric(sourceValues, targetValues, maxDistance) <= maxDistance) {
            links(sourceIndex)(targetIndex) :+= new Link(source = entities.source.uri, target = entities.target.uri, entities = Some(entities))
          }

          if (size > maxLinks && labelLinks > 100)
            generateLinksActivity.cancelExecution()
        }

        None
      }

      val id = Identifier.random

      val required = false

      val weight = 1

      val indexing = true

      private val sourceInputs = paths.source.map(p => PathInput(path = p))

      private val targetInputs = paths.target.map(p => PathInput(path = p))

      def index(entity: Entity, sourceOrTarget: Boolean, limit: Double): Index = {
        val inputs = if(sourceOrTarget) sourceInputs else targetInputs

        val index = inputs.map(i => i(entity)).map(metric.index(_, maxDistance).crop(maxIndices)).reduce(_ merge _)

        index
      }

      def children = Seq.empty

      def withChildren(newChildren: Seq[Operator]): Operator = ???
    }
  }
}