package de.fuberlin.wiwiss.silk.learning.active

import de.fuberlin.wiwiss.silk.datasource.Source
import de.fuberlin.wiwiss.silk.evaluation.ReferenceEntities
import de.fuberlin.wiwiss.silk.learning.individual.Population
import de.fuberlin.wiwiss.silk.util.task.ValueTask
import de.fuberlin.wiwiss.silk.util.DPair
import de.fuberlin.wiwiss.silk.entity.{Link, Path}
import de.fuberlin.wiwiss.silk.config.LinkSpecification
import de.fuberlin.wiwiss.silk.learning.generation.LinkageRuleGenerator
import de.fuberlin.wiwiss.silk.learning.reproduction.ReproductionTask
import de.fuberlin.wiwiss.silk.learning.cleaning.CleanPopulationTask
import de.fuberlin.wiwiss.silk.linkagerule.{Operator, LinkageRule}
import de.fuberlin.wiwiss.silk.linkagerule.similarity.{Comparison, Aggregation}
import de.fuberlin.wiwiss.silk.linkagerule.input.{PathInput, TransformInput}
import de.fuberlin.wiwiss.silk.learning.{LearningInput, LearningTask, LearningConfiguration}

class ActiveLearningTask(sources: Traversable[Source],
                         linkSpec: LinkSpecification,
                         paths: DPair[Seq[Path]],
                         referenceEntities: ReferenceEntities = ReferenceEntities.empty,
                         var pool: Traversable[Link] = Traversable.empty,
                         var population: Population = Population()) extends ValueTask[Seq[Link]](Seq.empty) {

  def links = value.get

  override protected def execute(): Seq[Link] = {
    //Build unlabeled pool
    if(pool.isEmpty) {
      pool = executeSubTask(new GeneratePoolTask(sources, linkSpec, paths), 0.8)
    }

    //Build population
    val config = LearningConfiguration.load()
    val generator = LinkageRuleGenerator(referenceEntities, config.components)

    if(population.isEmpty) {
      if(referenceEntities.isDefined) {
        val task = new LearningTask(LearningInput(referenceEntities), config)
        population = task().population
      } else {
        updateStatus("Generating population")
        population = new PopulationFromSample(pool).evaluate()
        population = FilterPopulation(population, pool)
      }
    }
    else if(referenceEntities.isDefined)  {
      for(i <- 1 to 3) {
        population = executeSubTask(new ReproductionTask(population, referenceEntities, generator, config), 0.9 + i / 3.0)
      }
      println("P:" + referenceEntities.positive.size + " N: " + referenceEntities.negative.size)
    }

    //Sample links
    updateStatus("Sampling", 0.9)

    val valLinks = new SampleFromPopulationTask(population, pool.toSeq, referenceEntities).apply()
    value.update(valLinks)

    //Clean population
    if(referenceEntities.isDefined)
      population = executeSubTask(new CleanPopulationTask(population, referenceEntities, generator))

    //computePropertyDisagreement(population)

    valLinks
  }

  private def computePropertyDisagreement(p: Population) {
    val link = links.head

    for(individual <- p.individuals) {
      val rule = individual.node.build
      println(formatRule(rule) + " " + individual.fitness.mcc + " " + rule(link.entities.get))
    }
  }

  private def formatRule(rule: LinkageRule) = {
    def collectPaths(op: Operator): Seq[Path] = op match {
      case agg: Aggregation => agg.operators.flatMap(collectPaths)
      case cmp: Comparison => cmp.inputs.flatMap(collectPaths)
      case t: TransformInput => t.inputs.flatMap(collectPaths)
      case i: PathInput => Seq(i.path)
    }

    val paths = collectPaths(rule.operator.get)
    val shortPaths  = paths.map(_.serialize.split("[/#]").last.init)

    shortPaths.mkString(" ")
  }
}

object ActiveLearningTask {
  def empty = new ActiveLearningTask(Traversable.empty, LinkSpecification(), DPair.fill(Seq.empty), ReferenceEntities.empty)
}