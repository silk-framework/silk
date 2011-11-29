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

package de.fuberlin.wiwiss.silk.workbench.learning

import de.fuberlin.wiwiss.silk.util.task.Task
import de.fuberlin.wiwiss.silk.evaluation.{LinkageRuleEvaluator, ReferenceEntities}
import de.fuberlin.wiwiss.silk.datasource.Source
import de.fuberlin.wiwiss.silk.config.LinkSpecification
import de.fuberlin.wiwiss.silk.util.DPair
import de.fuberlin.wiwiss.silk.entity.{Path, Link}
import de.fuberlin.wiwiss.silk.learning.LearningConfiguration
import java.io.{FileWriter, BufferedWriter}
import math.pow
import de.fuberlin.wiwiss.silk.learning.active.CompleteReferenceLinks._
import de.fuberlin.wiwiss.silk.learning.active.{CompleteReferenceLinks, ActiveLearningTask}
import de.fuberlin.wiwiss.silk.learning.active.linkselector.{UncertaintySelector, ProjLink, KullbackLeiblerDivergenceSelector}

class ActiveLearningEvaluator(config: LearningConfiguration,
                              sources: Traversable[Source],
                              linkSpec: LinkSpecification,
                              paths: DPair[Seq[Path]],
                              validationEntities: ReferenceEntities) extends Task[Unit] {

  override def execute() {
    var referenceEntities = ReferenceEntities()

    val positiveValLinks = for((link, entityPair) <- validationEntities.positive) yield link.update(entities = Some(entityPair))
    val negativeValLinks = for((link, entityPair) <- validationEntities.negative) yield link.update(entities = Some(entityPair))
    var pool: Traversable[Link] = positiveValLinks ++ negativeValLinks //Traversable[Link]()
    var population = CurrentPopulation()

    for(i <- 0 to 150) {
      val task =
        new ActiveLearningTask(
          config = config,
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
      if(valScores.fMeasure > 0.999) return

      val completeEntities = CompleteReferenceLinks(referenceEntities, pool, population)

      //Write visualization
      val bestFitness = population.bestIndividual.fitness
      val topIndividuals = population.individuals.toSeq.filter(_.fitness >= bestFitness * 0.1).sortBy(-_.fitness)
      val rules = topIndividuals.map(_.node.build).toSeq

      val kl = new UncertaintySelector()
      val proj = kl.projection(rules, completeEntities)

      val positiveLinks = for((link, entityPair) <- completeEntities.positive) yield link.update(entities = Some(entityPair))
      val negativeLinks = for((link, entityPair) <- completeEntities.negative) yield link.update(entities = Some(entityPair))

      val unlabeled = pool.map(proj)
      val positive = positiveLinks.map(proj)
      val negative = negativeLinks.map(proj)

      val rank = kl.ranking(rules, unlabeled, positive, negative)

      val writer = new BufferedWriter(new FileWriter("iter" + i + ".csv"))
      writer.write("id,dist,type," + (1 to rules.size).map("a" + _).mkString(",") + "\n")
      
      for(ProjLink(link, vector) <- positive) {
        writer.write((link.source + link.target).replace(",", "%2C") + ",0.0,pos," + vector.mkString(",") + "\n")
      }

      for(ProjLink(link, vector) <- negative) {
        writer.write((link.source + link.target).replace(",", "%2C") + ",0.0,neg," + vector.mkString(",") + "\n")
      }

      for(p @ ProjLink(link, vector) <- unlabeled) {
        val isSelected = link == task.links.head
        val matchTrain = rule(link.entities.get) > 0.0
        val matchVal = validationEntities.positive.contains(link)
        val label = (matchTrain, matchVal) match {
          case (_, _) if isSelected => "sel"
          case (false, false) => "tn"
          case (false, true) => "fn"
          case (true, false) => "fp"
          case (true, true) => "tp"
        }
        writer.write((link.source + link.target).replace(",", "%2C") + "," +  rank(p) + "," + label +"," + vector.mkString(",") + "\n")
      }

      writer.close()
                                
      //Evaluate new link
      val link = task.links.head
      println(link.entities.get.source.evaluate(0).toString + " " + link.entities.get.source.evaluate(1).toString + " = " +
        link.entities.get.target.evaluate(19).toString + " " + link.entities.get.target.evaluate(1).toString)
      if(validationEntities.positive.contains(link)) {
        println(link + " added to positive")
        referenceEntities = referenceEntities.withPositive(link.entities.get)
      }
      else {
        println(link + " added to negative")
        referenceEntities = referenceEntities.withNegative(link.entities.get)
      }
    }
  }


}