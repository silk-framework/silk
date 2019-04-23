package org.silkframework.rule.execution

import org.apache.jena.graph.NodeFactory
import org.silkframework.entity.{EntitySchema, Path, Restriction}
import org.silkframework.entity.Restriction.{And, CustomOperator}
import org.silkframework.entity.rdf.{SparqlEntitySchema, SparqlPathBuilder}
import org.silkframework.rule.input.{PathInput, TransformInput}
import org.silkframework.rule.plugins.distance.equality.{EqualityMetric, InequalityMetric}
import org.silkframework.rule.plugins.transformer.value.ConstantTransformer
import org.silkframework.rule.similarity.Comparison
import org.silkframework.rule.util.JenaSerializationUtil
import org.silkframework.rule.{BooleanLinkageRule, CnfBooleanLeafComparison, CnfBooleanLeafNot, LinkageRule}

/**
  * Converts comparison rules trees to SPARQL filters.
  */
object ComparisonToRestrictionConverter {
  /**
    * Converts a comparison tree to a data source restriction if it matches supported patterns.
    *
    * @param comparison        The comparison that should be converted.
    * @param sourceOrTarget    If true this will generate the restriction for the source part of the comparison,
    *                          i.e. the source dataset. Else it will generate it for the target dataset.
    * @param invertRestriction if true the restriction matching the comparison tree will be inverted. e.g. if the
    *                          comparison if the child of a boolean NOT operator.
    */
  def convertComparison(comparison: Comparison, sourceOrTarget: Boolean, invertRestriction: Boolean): Option[DataSourceRestriction] = {
    // FIXME: Handle more filter-like patterns
    // FIXME: When adding more patterns, refactor match expression, e.g. Map[<ComparisonType>, Seq[<FilterPatternMatcher>]]
    val dataSourceInput = if(sourceOrTarget) comparison.inputs.source else comparison.inputs.target // the input from the respective data source
    val comparisonInput = if(sourceOrTarget) comparison.inputs.target else comparison.inputs.source // the input "value" to compare against
    val restriction = (comparison.metric, dataSourceInput, comparisonInput) match {
      case (_: EqualityMetric, PathInput(_, path), TransformInput(_, ConstantTransformer(constant), Seq())) =>
        Some(DataSourceEqualityRestriction(path, constant))
      case (_: InequalityMetric, PathInput(_, path), TransformInput(_, ConstantTransformer(constant), Seq())) =>
        Some(DataSourceInequalityRestriction(path, constant))
      case _ =>
        None
    }
    if(invertRestriction) restriction.flatMap(_.inverted) else restriction
  }

  /** Turns a linkage rule into a SPARQL restriction if that is possible.
    *
    * Algorithm:
    *
    * 1. Turn linkage rule into a boolean linkage rule
    * 2. Convert boolean linkage rule into CNF
    * 3. For each disjunctive clause in the CNF check if all of it's children can be converted to a data source restriction.
    * 4. For all resulting data source restrictions, convert each part to a SPARQL filter restriction and combine them as disjunction.
    * 5. Combine all disjunctions in a conjunction.
    *
    * Since every disjunction in the CNF is a necessity, each resulting restriction will further reduce the result set.
    **/
  def linkageRuleToSparqlFilter(linkageRule: LinkageRule,
                                subject: String,
                                variablePrefix: String,
                                sourceOrTarget: Boolean): Option[CustomOperator] = {
    BooleanLinkageRule(linkageRule). // Step 1
        map(_.toCNF). // Step 2
        flatMap { cnfBoolRule =>
      val disjunctionSparqlRestrictions = (for((disjunction, idx) <- cnfBoolRule.orClauses.zipWithIndex) yield {
        val dataSourceRestrictions = disjunction.leaves map {
          case cnfNot: CnfBooleanLeafNot =>
            convertComparison(cnfNot.booleanComparison.comparison, sourceOrTarget, invertRestriction = true)
          case CnfBooleanLeafComparison(comparisonOperator) =>
            convertComparison(comparisonOperator.comparison, sourceOrTarget, invertRestriction = false)
        }
        val variablePrefixForDisjunction = variablePrefix + idx + "_"
        if(dataSourceRestrictions.forall(_.isDefined)) { // Step 3: Check if all parts were converted
          val sparqlFilterRestrictions = dataSourceRestrictions.flatten.zipWithIndex.
              map { case (dataSourceRestriction, restrictionIdx) =>
                val restrictionVariablePrefix = variablePrefixForDisjunction + restrictionIdx + "_"
                dataSourceRestriction.toSparqlFilter(subject, restrictionVariablePrefix) // Step 4: convert to SPARQL filter restriction
              }
          SparqlFilterRestriction.disjunction(sparqlFilterRestrictions) // Step 4: Combine all SPARQL filter restrictions as disjunction
        } else {
          None
        }
      }).flatten
      val finalSparqlFilterRestriction = SparqlFilterRestriction.conjunction(disjunctionSparqlRestrictions) // Step 5
      finalSparqlFilterRestriction.map(filter => CustomOperator(filter.toSparql))
    }
  }

  def extendEntitySchemaWithLinkageRuleRestriction(entitySchema: EntitySchema,
                                                   linkageRule: LinkageRule,
                                                   sourceOrTarget: Boolean): EntitySchema = {
    generateSparqlRestriction(linkageRule, sourceOrTarget) match {
      case Some(newSparqlRestriction) =>
        val updatedFilterOperator = entitySchema.filter.operator match {
          case Some(operator) => And(Seq(operator, newSparqlRestriction))
          case None => newSparqlRestriction
        }
        entitySchema.copy(filter = Restriction(Some(updatedFilterOperator)))
      case None => entitySchema
    }
  }


  private def generateSparqlRestriction(linkageRule: LinkageRule,
                                        sourceOrTarget: Boolean): Option[CustomOperator] = {
    ComparisonToRestrictionConverter.linkageRuleToSparqlFilter(
      linkageRule,
      SparqlEntitySchema.variable,
      variablePrefix = "generatedFilterVar",
      sourceOrTarget = sourceOrTarget
    )
  }
}

sealed trait DataSourceRestriction {
  def toSparqlFilter(subjectVar: String,
                     generatedVarPrefix: String): SparqlFilterRestriction

  /** The NOT version of this restriction. None if this restriction cannot be inverted. */
  def inverted: Option[DataSourceRestriction]

  /**
    * Generates the SPARQL representation of a Path.
    * @param subjectVar          The subject of the path
    * @param generatedVarPrefix  A prefix that will be used to generate variable names
    * @return A tuple of the generated SPARQL and the object variable name of the path (sparql, object var name)
    */
  def convertPath(subjectVar: String, path: Path, generatedVarPrefix: String): (String, String) = {
    val valueVar = s"${generatedVarPrefix}Value"
    val sparql = SparqlPathBuilder.path(path,
      subject = "?" + subjectVar,
      value = "?" + valueVar,
      tempVarPrefix = s"?${generatedVarPrefix}Inter",
      filterVarPrefix = s"?${generatedVarPrefix}Filter")
    (sparql, valueVar)
  }
}

/** An equality (=) filter restriction on the lexical representation of a value */
case class DataSourceEqualityRestriction(path: Path, value: String) extends DataSourceRestriction {
  override def toSparqlFilter(subjectVar: String,
                              generatedVarPrefix: String): SparqlFilterRestriction = {
    val (pathSparql, valueVar) = convertPath(subjectVar, path, generatedVarPrefix)
    SparqlFilterRestriction(
      sparqlPattern = pathSparql,
      filterExpression = s"STR(?$valueVar) = ${JenaSerializationUtil.serializeSingleNode(NodeFactory.createLiteral(value))}"
    )
  }

  override def inverted: Option[DataSourceRestriction] = Some(DataSourceInequalityRestriction(path, value))
}

case class DataSourceInequalityRestriction(path: Path, value: String) extends DataSourceRestriction {
  override def toSparqlFilter(subjectVar: String,
                              generatedVarPrefix: String): SparqlFilterRestriction = {
    val (pathSparql, valueVar) = convertPath(subjectVar, path, generatedVarPrefix)
    SparqlFilterRestriction(
      sparqlPattern = pathSparql,
      filterExpression = s"STR(?$valueVar) != ${JenaSerializationUtil.serializeSingleNode(NodeFactory.createLiteral(value))}"
    )
  }

  override def inverted: Option[DataSourceRestriction] = Some(DataSourceEqualityRestriction(path, value))
}

/** A representation of a restriction in SPARQL.
  *
  * @param sparqlPattern    The SPARQL pattern that selects the necessary values.
  * @param filterExpression The filter expression that can be used inside a SPARQL FILTER expression.
  **/
case class SparqlFilterRestriction(sparqlPattern: String, filterExpression: String) {
  def toSparql: String = {
    s"""$sparqlPattern
       |FILTER ($filterExpression)""".stripMargin
  }
}

object SparqlFilterRestriction {
  /** Combine the restrictions in a disjunction */
  def disjunction(filterRestrictions: Seq[SparqlFilterRestriction]): Option[SparqlFilterRestriction] = {
    if(filterRestrictions.isEmpty) {
      None
    } else {
      val sparqlPattern = filterRestrictions.map(_.sparqlPattern).mkString
      val sparqlFilter = filterRestrictions.map(_.filterExpression).mkString("(", " || ", ")")
      Some(SparqlFilterRestriction(sparqlPattern, sparqlFilter))
    }
  }

  /** Combine the restrictions in a conjunction */
  def conjunction(filterRestrictions: Seq[SparqlFilterRestriction]): Option[SparqlFilterRestriction] = {
    if(filterRestrictions.isEmpty) {
      None
    } else {
      val sparqlPattern = filterRestrictions.map(_.sparqlPattern).mkString
      val sparqlFilter = filterRestrictions.map(_.filterExpression).mkString("(", " && ", ")")
      Some(SparqlFilterRestriction(sparqlPattern, sparqlFilter))
    }
  }
}