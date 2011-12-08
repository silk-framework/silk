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

package de.fuberlin.wiwiss.silk.learning.active

import de.fuberlin.wiwiss.silk.util.task.Task
import de.fuberlin.wiwiss.silk.datasource.Source
import de.fuberlin.wiwiss.silk.config.LinkSpecification
import de.fuberlin.wiwiss.silk.util.DPair
import de.fuberlin.wiwiss.silk.entity.{Path, Link}
import de.fuberlin.wiwiss.silk.learning.LearningConfiguration
import java.io.{FileWriter, BufferedWriter}
import de.fuberlin.wiwiss.silk.learning.individual.Population
import de.fuberlin.wiwiss.silk.evaluation.{EvaluationResult, LinkageRuleEvaluator, ReferenceEntities}
import linkselector._

class ActiveLearningEvaluator(config: LearningConfiguration,
                              sources: Traversable[Source],
                              linkSpec: LinkSpecification,
                              paths: DPair[Seq[Path]],
                              validationEntities: ReferenceEntities) extends Task[Unit] {

  val numRuns = 10

  val selectors =
    Map(
      "false" -> new KullbackLeiblerDivergenceSelector(false),
      "true" -> new KullbackLeiblerDivergenceSelector(true)
      //"test" -> new EntropySelector()
      //"test" -> JensenShannonDivergenceSelector(true)
    )

  override def execute() {
    for((name, selector) <- selectors) run(name, selector)
  }
  
  private def run(name: String, selector: LinkSelector) {
    //Execute runs
    val results = List.fill(numRuns)(singleRun(selector))

    //Write results
    val writer = new BufferedWriter(new FileWriter("evaluationresults" + name + ".txt"))
    writer.write(RunStatistic.merge(results).format)
    writer.write("\nMean iterations for 90%: " + RunStatistic.meanIterations(results, 0.9))
    writer.write("\nMean iterations for 95%: " + RunStatistic.meanIterations(results, 0.95))
    writer.write("\nMean iterations for 100%: " + RunStatistic.meanIterations(results, 0.999))
    writer.close()
  }
  
  private def singleRun(selector: LinkSelector): RunStatistic = {
    var referenceEntities = ReferenceEntities()

    val positiveValLinks = for((link, entityPair) <- validationEntities.positive) yield link.update(entities = Some(entityPair))
    val negativeValLinks = for((link, entityPair) <- validationEntities.negative) yield link.update(entities = Some(entityPair))
    var pool: Traversable[Link] = positiveValLinks ++ negativeValLinks
    var population = Population()
    
    //Holds the validation result from each iteration
    var scores = List[Double]()
    
    for(i <- 0 to 1000) {
      val task =
        new ActiveLearningTask(
          config = config.copy(active = ActiveLearningConfiguration(selector) ,params = config.params.copy(seed = false)),
          sources = sources,
          linkSpec = linkSpec,
          paths = paths,
          referenceEntities = referenceEntities,
          pool = pool,
          population = population
        )

      task()

      pool = task.pool
      population = task.population

      //Evaluate performance of learned linkage rule
      val rule = population.bestIndividual.node.build
      val trainScores = LinkageRuleEvaluator(rule, referenceEntities)
      val valScores = LinkageRuleEvaluator(rule, validationEntities)
      println(i + " - " + trainScores)
      println(i + " - " + valScores)
      scores ::= valScores.fMeasure
      if(valScores.fMeasure > 0.999) {
        return RunStatistic(scores.reverse)
      }
                                
      //Evaluate new link
      val link = task.links.head
      if(validationEntities.positive.contains(link)) {
        println(link + " added to positive")
        referenceEntities = referenceEntities.withPositive(link.entities.get)
      }
      else {
        println(link + " added to negative")
        referenceEntities = referenceEntities.withNegative(link.entities.get)
      }
    }
    
    RunStatistic(scores.reverse)
  }

  private case class RunStatistic(results: List[Double]) {
    def format() = {
      "F-measure\n" + results.mkString("\n")
    }
   
    /**
     * Compute the number of iterations needed to reach a specific F-measure.
     */
    def iterations(fMeasure: Double): Int = {
      results.indexWhere(_ >= fMeasure) match {
        case -1 => throw new IllegalArgumentException("Target F-measure " + fMeasure + " never reached.")
        case i => i
      }
    }
  }

  private object RunStatistic {
    def merge(statistics: List[RunStatistic]) = {
      val maxIterations = statistics.map(_.results.size).max 
      val fMeasures = statistics.map(_.results.padTo(maxIterations, 1.0))
      val meanfMeasures = fMeasures.transpose.map(d => d.sum / d.size)
      RunStatistic(meanfMeasures)
    }

    /**
     * Compute the average number of iterations needed to reach a specific F-measure.
     */
    def meanIterations(statistics: List[RunStatistic], fMeasure: Double): Double = {
      statistics.map(_.iterations(fMeasure)).sum.toDouble / statistics.size
    }
  }
}