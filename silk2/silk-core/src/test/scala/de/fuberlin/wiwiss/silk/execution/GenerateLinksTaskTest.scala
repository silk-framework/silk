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
import methods._
import de.fuberlin.wiwiss.silk.plugins.Plugins
import de.fuberlin.wiwiss.silk.evaluation.ReferenceLinksReader
import io.Source
import java.util.logging.Level
import de.fuberlin.wiwiss.silk.config.RuntimeConfig
import methods.Blocking
import methods.MultiBlock
import methods.SortedBlocks

/**
 * This test evaluates the GenerateLinksTask with different execution methods.
 */
object GenerateLinksTaskTest {

  Plugins.register()

  /** Directory of the data set */
  private val dataset = Dataset("Names", "names/config.xml", "names/links.nt")

  private val sourceKey = Path.parse("?a/<label>")
  private val targetKey = Path.parse("?b/<label>")

  private val tests =
    Config("Full", Full()) ::
    Config("Blocking", Blocking(sourceKey, targetKey)) ::
    Config("SortedBlocks", SortedBlocks(sourceKey, targetKey)) ::
    Config("StringMap", StringMap(sourceKey, targetKey, 2)) ::
    Config("MultiBlock", MultiBlock()) ::
    Nil

  def main(args: Array[String]) {
    val fullLinks = dataset.loadLinks

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
    val config = dataset.loadConfig

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

  /**
   * Specifies an evaluation data set.
   *
   * @param name The name of the data set
   * @param configFile The path of the linking config file.
   * @param referenceLinks The path of the reference links.
   */
  private case class Dataset(name: String, configFile: String, referenceLinks: String) {
    def loadConfig: LinkingConfig = {
      val stream = getClass.getClassLoader.getResourceAsStream(configFile)
      LinkingConfig.load(stream)
    }

    def loadLinks: Set[Link] = {
      val stream = getClass.getClassLoader.getResourceAsStream(referenceLinks)
      ReferenceLinksReader.readNTriples(Source.fromInputStream(stream)).positive
    }
  }

  /**
   * Specifies a configuration that is evaluated.
   *
   * @param name The name of this configuration
   * @param executionMethod The execution method used in this configuration
   */
  private case class Config(name: String, executionMethod: ExecutionMethod)

  /**
   * The result of executing a configuration.
   *
   * @param name The name of the executed configuration
   * @param pairsCompleteness The pairs completeness
   * @param pairsQuality The pairs quality
   * @param runtime The runtime in ms
   */
  private case class Result(name: String, pairsCompleteness: Double, pairsQuality: Double, runtime: Double) {
    override def toString =
      s"$name: Pairs Completeness = $pairsCompleteness Pairs Quality = $pairsQuality Runtime = $runtime"
  }
}