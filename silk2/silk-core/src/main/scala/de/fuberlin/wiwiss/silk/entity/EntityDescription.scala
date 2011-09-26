package de.fuberlin.wiwiss.silk.entity

import de.fuberlin.wiwiss.silk.linkspec._
import similarity.{SimilarityOperator, Comparison, Aggregation}
import input.{TransformInput, PathInput, Input}
import de.fuberlin.wiwiss.silk.util.SourceTargetPair
import xml.Node
import de.fuberlin.wiwiss.silk.config.Prefixes

case class EntityDescription(variable: String, restrictions: SparqlRestriction, paths: IndexedSeq[Path]) {
  def pathIndex(path: Path) = {
    paths.indexWhere(_ == path) match {
      case -1 => throw new NoSuchElementException("Path " + path + " not found on entity.")
      case index => index
    }
  }

  def merge(other: EntityDescription) = {
    require(variable == other.variable)
    require(restrictions == other.restrictions)

    copy(paths = (paths ++ other.paths).distinct)
  }

  def toXML = {
    <EntityDescription>
      <Variable>{variable}</Variable>
      {restrictions.toXML}
      <Paths> {
        for (path <- paths) yield {
          <Path>{path.serialize(Prefixes.empty)}</Path>
        }
      }
      </Paths>
    </EntityDescription>
  }
}

object EntityDescription {
  def fromXML(node: Node) = {
    new EntityDescription(
      variable = (node \ "Variable").text.trim,
      restrictions = SparqlRestriction.fromXML(node \ "Restrictions" head)(Prefixes.empty),
      paths = for (pathNode <- (node \ "Paths" \ "Path").toIndexedSeq[Node]) yield Path.parse(pathNode.text.trim)
    )
  }

  def retrieve(linkSpec: LinkSpecification): SourceTargetPair[EntityDescription] = {
    val sourceVar = linkSpec.datasets.source.variable
    val targetVar = linkSpec.datasets.target.variable

    val sourceRestriction = linkSpec.datasets.source.restriction
    val targetRestriction = linkSpec.datasets.target.restriction

    val sourcePaths = linkSpec.rule.operator match {
      case Some(operator) => collectPaths(sourceVar)(operator)
      case None => Set[Path]()
    }

    val targetPaths = linkSpec.rule.operator match {
      case Some(operator) => collectPaths(targetVar)(operator)
      case None => Set[Path]()
    }

    val sourceEntityDesc = new EntityDescription(sourceVar, sourceRestriction, sourcePaths.toIndexedSeq)
    val targetEntityDesc = new EntityDescription(targetVar, targetRestriction, targetPaths.toIndexedSeq)

    SourceTargetPair(sourceEntityDesc, targetEntityDesc)
  }

  private def collectPaths(variable: String)(operator: SimilarityOperator): Set[Path] = operator match {
    case aggregation: Aggregation => aggregation.operators.flatMap(collectPaths(variable)).toSet
    case comparison: Comparison => {
      val sourcePaths = collectPathsFromInput(variable)(comparison.inputs.source)
      val targetPaths = collectPathsFromInput(variable)(comparison.inputs.target)
      (sourcePaths ++ targetPaths).toSet
    }
  }

  private def collectPathsFromInput(variable: String)(param: Input): Set[Path] = param match {
    case p: PathInput if p.path.variable == variable => Set(p.path)
    case p: TransformInput => p.inputs.flatMap(collectPathsFromInput(variable)).toSet
    case _ => Set()
  }
}
