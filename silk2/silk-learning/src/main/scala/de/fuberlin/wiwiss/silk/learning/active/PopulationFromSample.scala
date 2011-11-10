package de.fuberlin.wiwiss.silk.learning.active

import de.fuberlin.wiwiss.silk.learning.LearningInput._
import de.fuberlin.wiwiss.silk.learning.generation.LinkageRuleGenerator._
import de.fuberlin.wiwiss.silk.util.task.Task._
import de.fuberlin.wiwiss.silk.learning.{LearningInput, LearningConfiguration}
import de.fuberlin.wiwiss.silk.learning.generation.{LinkageRuleGenerator, GeneratePopulationTask}
import de.fuberlin.wiwiss.silk.evaluation.ReferenceEntities
import de.fuberlin.wiwiss.silk.linkagerule.{Operator, LinkageRule}
import de.fuberlin.wiwiss.silk.entity.{Path, Link}
import de.fuberlin.wiwiss.silk.linkagerule.similarity.{Comparison, Aggregation}
import de.fuberlin.wiwiss.silk.linkagerule.input.{PathInput, TransformInput}
import util.Random
import de.fuberlin.wiwiss.silk.util.DPair
import de.fuberlin.wiwiss.silk.learning.individual.{Population, Individual}

private class PopulationFromSample(links: Traversable[Link]) {

  def evaluate() = {
    val input = LearningInput()//TODO Include current linkage rule?
    val config = LearningConfiguration.load()
    val generator = LinkageRuleGenerator(ReferenceEntities.fromEntities(links.map(_.entities.get), Nil), config.components)
    val task = new GeneratePopulationTask(input, generator, config)
    //val population = executeSubTask(task, 5.0, true)
    task()
  }


}