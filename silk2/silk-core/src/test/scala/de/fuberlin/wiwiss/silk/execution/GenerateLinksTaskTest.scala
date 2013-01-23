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

package de.fuberlin.wiwiss.silk.execution

import de.fuberlin.wiwiss.silk.plugins.datasource.CsvDataSource
import de.fuberlin.wiwiss.silk.config.{RuntimeConfig, Dataset, LinkSpecification}
import de.fuberlin.wiwiss.silk.util.{Identifier, DPair}
import de.fuberlin.wiwiss.silk.datasource.Source
import de.fuberlin.wiwiss.silk.entity.{Path, Link, SparqlRestriction}
import de.fuberlin.wiwiss.silk.linkagerule.LinkageRule
import de.fuberlin.wiwiss.silk.linkagerule.similarity.Comparison
import de.fuberlin.wiwiss.silk.plugins.metric.LevenshteinDistance
import de.fuberlin.wiwiss.silk.linkagerule.input.PathInput
import methods.{Blocking, MultiBlock, Full}


object GenerateLinksTaskTest {

  private val sourcePath = Path.parse("?a/<label>")
  private val targetPath = Path.parse("?b/<label>")

  def main(args: Array[String]) {
    val fullLinks = run(RuntimeConfig(executionMethod = Full()))
    val multiBlockLinks = run(RuntimeConfig(executionMethod = Blocking(sourcePath, targetPath), indexingOnly = true))

    val missedLinks = fullLinks -- multiBlockLinks
    val redundantLinks = multiBlockLinks -- fullLinks

    println("Full Links: " + fullLinks.size)
    println("Indexed Links: " + multiBlockLinks.size)
    println("Missed Links: " + missedLinks.size)
    println("Redundant Links: " + redundantLinks.size)
  }

  private def run(runtimeConfig: RuntimeConfig): Set[Link] = {
    // Sources to match
    val cl = getClass.getClassLoader
    val source1 = Source(Identifier.random, CsvDataSource(cl.getResource("source1.txt").toString, "label"))
    val source2 = Source(Identifier.random, CsvDataSource(cl.getResource("source2.txt").toString, "label"))

    val sourceDataset = Dataset(source1.id, "a", SparqlRestriction.fromSparql("a", ""))
    val targetDataset = Dataset(source2.id, "b", SparqlRestriction.fromSparql("b", ""))

    // Linkage Rule
    val linkageRule =
      LinkageRule(
        Comparison(
          metric = LevenshteinDistance(),
          threshold = 2.0,
          inputs = DPair(PathInput(path = sourcePath), PathInput(path = targetPath))
      )
    )

    // Execute Matching
    val task =
      new GenerateLinksTask(
        sources = Seq(source1, source2),
        linkSpec = LinkSpecification(datasets = DPair(sourceDataset, targetDataset), rule = linkageRule),
        runtimeConfig = runtimeConfig
      )

    val links = task()
    links.toSet
  }

}