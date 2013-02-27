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
import de.fuberlin.wiwiss.silk.config.{LinkingConfig, RuntimeConfig, Dataset, LinkSpecification}
import de.fuberlin.wiwiss.silk.util.{Identifier, DPair}
import de.fuberlin.wiwiss.silk.datasource.Source
import de.fuberlin.wiwiss.silk.entity.{Path, Link, SparqlRestriction}
import de.fuberlin.wiwiss.silk.linkagerule.LinkageRule
import de.fuberlin.wiwiss.silk.linkagerule.similarity.Comparison
import de.fuberlin.wiwiss.silk.plugins.metric.LevenshteinDistance
import de.fuberlin.wiwiss.silk.linkagerule.input.PathInput
import methods.{StringMap, Blocking, MultiBlock, Full}
import java.util.logging.Level
import de.fuberlin.wiwiss.silk.plugins.Plugins

object GenerateLinksTaskTest {

  Plugins.register()

//  private val sourceInput = "names/source1.txt"
//  private val targetInput = "names/source2.txt"
  private val sourceInput = "cities/dbpedia.csv"
  private val targetInput = "cities/linkedgeodata.csv"

  private val sourcePath = Path.parse("?a/<label>")
  private val targetPath = Path.parse("?b/<label>")

  private val tests =
    //Test("Blocking", Blocking(sourcePath, targetPath)) ::
    //Test("StringMap", StringMap(sourcePath, targetPath)) ::
    Test("MultiBlock", MultiBlock()) :: Nil

  def main(args: Array[String]) {
    val fullLinks = run(RuntimeConfig(executionMethod = MultiBlock()))

//    val testResults =
//      for (test <- tests) yield {
//        println("Running " + test.name + " test...")
//
//        val startTime = System.currentTimeMillis
//        val indexingLinks = run(RuntimeConfig(executionMethod = test.executionMethod, indexingOnly = true))
//        val missedLinks = fullLinks -- indexingLinks
//        val redundantLinks = indexingLinks -- fullLinks
//
//        println("Full Links: " + fullLinks.size)
//        println("Indexed Links: " + indexingLinks.size)
//        println("Missed Links: " + missedLinks.size)
//        println("Redundant Links: " + redundantLinks.size)
//
//        Result(
//          name = test.name,
//          completeness = (1.0 - missedLinks.size.toDouble / fullLinks.size),
//          runtime = System.currentTimeMillis - startTime
//        )
//
//        //println("Pairs Completeness: " + )
//      }
//
//    testResults.foreach(println)

    //TODO for StringMap: add the number of comparisons needed for computing the threshold
  }

  private def run(runtimeConfig: RuntimeConfig): Set[Link] = {
//    // Sources to match
//    val cl = getClass.getClassLoader
//    val source1 = Source(Identifier.random, CsvDataSource(cl.getResource(sourceInput).toString, "uri,label,coordinates"))
//    val source2 = Source(Identifier.random, CsvDataSource(cl.getResource(targetInput).toString, "uri,label,coordinates"))
//
//    val sourceDataset = Dataset(source1.id, "a", SparqlRestriction.fromSparql("a", ""))
//    val targetDataset = Dataset(source2.id, "b", SparqlRestriction.fromSparql("b", ""))
//
//    // Linkage Rule
//    val linkageRule =
//      LinkageRule(
//        Comparison(
//          metric = LevenshteinDistance(),
//          threshold = 0.0,
//          inputs = DPair(PathInput(path = sourcePath), PathInput(path = targetPath))
//      )
//    )

    val config = LinkingConfig.load(getClass.getClassLoader.getResourceAsStream("cities/config.xml"))

    // Execute Matching
    val task =
      new GenerateLinksTask(
        sources = config.sources,
        linkSpec = config.linkSpecs.head,
        outputs = config.outputs,
        runtimeConfig = runtimeConfig
      )

    //task.progressLogLevel = Level.FINEST
    //task.statusLogLevel = Level.FINEST

    val links = task()
    links.toSet
  }

  private case class Test(name: String, executionMethod: ExecutionMethod)

  private case class Result(name: String, completeness: Double, runtime: Double)
}