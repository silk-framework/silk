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

import de.fuberlin.wiwiss.silk.config.{LinkingConfig, RuntimeConfig}
import de.fuberlin.wiwiss.silk.entity.{Path, Link}
import methods.{Blocking, StringMap, MultiBlock}
import de.fuberlin.wiwiss.silk.plugins.Plugins
import de.fuberlin.wiwiss.silk.evaluation.ReferenceLinksReader
import io.Source
import java.util.logging.Level

object GenerateLinksTaskTest {

  Plugins.register()

  /** Directory of the data set */
  //private val dataset = "names"
  private val dataset = "cities"

  private val sourcePath = Path.parse("?a/<name>")
  private val targetPath = Path.parse("?b/<name>")

  private val tests =
    //Test("Blocking", Blocking(sourcePath, targetPath)) ::
    //Test("StringMap", StringMap(sourcePath, targetPath)) ::
    Test("MultiBlock", MultiBlock()) ::
    Nil

  def main(args: Array[String]) {
    val fullLinks = Set[Link]() //ReferenceLinksReader.readNTriples(Source.fromInputStream(getClass.getClassLoader.getResourceAsStream(dataset + "/links.nt"))).positive

    val results =
      for (test <- tests) yield {
        println("Running " + test.name + " test...")

        val startTime = System.currentTimeMillis
        val foundLinks = run(RuntimeConfig(executionMethod = test.executionMethod, indexingOnly = true, logLevel = Level.FINE))
        val correctLinks = foundLinks intersect fullLinks
        val missedLinks = fullLinks -- foundLinks

//        println("Full Links: " + fullLinks.size)
//        println("Found Links: " + foundLinks.size)
//        println("Missed Links: " + missedLinks.size)

        Thread.sleep(5000)

        Result(
          name = test.name,
          pairsCompleteness = (1.0 - missedLinks.size.toDouble / fullLinks.size),
          pairsQuality = correctLinks.size.toDouble / foundLinks.size,
          runtime = System.currentTimeMillis - startTime
        )
      }

    results.foreach(println)
    //TODO for StringMap: add the number of comparisons needed for computing the threshold
  }

  private def run(runtimeConfig: RuntimeConfig): Set[Link] = {
    val config = LinkingConfig.load(getClass.getClassLoader.getResourceAsStream(dataset + "/config.xml"))

    // Execute Matching
    val task =
      new GenerateLinksTask(
        sources = config.sources,
        linkSpec = config.linkSpecs.head,
        outputs = config.outputs,
        runtimeConfig = runtimeConfig
      )

    val links = task()
    links.toSet
  }

  private case class Test(name: String, executionMethod: ExecutionMethod)

private case class Result(name: String, pairsCompleteness: Double, pairsQuality: Double, runtime: Double) {
  override def toString =
    s"$name: Pairs Completeness = $pairsCompleteness Pairs Quality = $pairsQuality Runtime = $runtime"
}
}