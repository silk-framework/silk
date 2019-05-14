package org.silkframework.rule.execution

import org.apache.jena.graph.NodeFactory
import org.silkframework.config.DefaultConfig
import org.silkframework.entity.Restriction.{And, CustomOperator}
import org.silkframework.entity.rdf.{SparqlEntitySchema, SparqlPathBuilder}
import org.silkframework.entity.{EntitySchema, Path, Restriction}
import org.silkframework.rule._
import org.silkframework.rule.input.{PathInput, TransformInput}
import org.silkframework.rule.plugins.distance.equality.{EqualityMetric, InequalityMetric}
import org.silkframework.rule.plugins.transformer.value.ConstantTransformer
import org.silkframework.rule.similarity.Comparison
import org.silkframework.rule.util.JenaSerializationUtil

/**
  * Converts comparison rules trees to SPARQL filters.
  */
object ComparisonToRestrictionConverter {
  final val PUSH_FILTER_INTO_DATA_SOURCE_KEY = "optimizations.linking.execution.pushFilters.enabled"
  final val REMOVE_INEQUALITY_CLAUSES_FROM_CNF_KEY = "optimizations.linking.execution.pushFilters.removeDisjunctionsWithInEqualities"

  private val cfg = DefaultConfig.instance()

  val pushFilterEnabled: Boolean = cfg.getBoolean(PUSH_FILTER_INTO_DATA_SOURCE_KEY)
  def removeInequalityClauses: Boolean = cfg.getBoolean(REMOVE_INEQUALITY_CLAUSES_FROM_CNF_KEY)

  /** Turns a linkage rule into a SPARQL restriction if that is possible.
    *
    * Algorithm:
    *
    * 1. Turn linkage rule into a boolean linkage rule
    * 2. Convert boolean linkage rule into CNF
    * 3. For each disjunctive clause in the CNF check if all of it's children can be converted to a data source restriction.
    * 4. For all resulting data source restrictions, a) convert each part to a SPARQL filter restriction and b) combine them as disjunction.
    * 5. Combine all disjunctions in a conjunction.
    *
    * Since every disjunction in the CNF is a necessity, each resulting restriction will further reduce the result set.
    *
    * @param linkageRule        The linkage rule to convert into a filter.
    * @param subject            The variable name of the subject of the restriction.
    * @param variablePrefix     The variable prefix in order to generate unique variable names for the filter.
    * @param sourceOrTarget     Is the restriction for the source or the target data source of the linkage rule.
    * @param removeInequalities If this is set to true all disjunctive clauses (OR) that contain at least one inequality
    *                           are removed.
    * */
  def linkageRuleToRestriction(linkageRule: LinkageRule,
                               subject: String,
                               variablePrefix: String,
                               sourceOrTarget: Boolean,
                               removeInequalities: Boolean = removeInequalityClauses): Option[CustomOperator] = {
    if (pushFilterEnabled) {
      try {
        BooleanLinkageRule(linkageRule).
            // Step 1: Turn linkage rule into a boolean linkage rule
            map(_.toCNF).
            flatMap { cnfBoolRule =>
              // Step 2: Convert boolean linkage rule into CNF
              convertCnfToRestriction(variablePrefix, cnfBoolRule, subject, sourceOrTarget, removeInequalities)
            }
      } catch {
        case _: IllegalArgumentException => None
      }
    } else {
      None
    }
  }

  private def convertCnfToRestriction(variablePrefix: String,
                                      cnfBoolRule: CnfBooleanAnd,
                                      subject: String,
                                      sourceOrTarget: Boolean,
                                      removeInequalities: Boolean): Option[CustomOperator] = {
    val disjunctionSparqlRestrictions = (for ((disjunction, idx) <- cnfBoolRule.orClauses.zipWithIndex) yield {
      val disjunctiveRestrictions = convertDisjunctionToRestriction(disjunction, sourceOrTarget)
      val variablePrefixForDisjunction = variablePrefix + idx + "_"
      // Step 3: Check if all parts were converted
      if (shouldBeConverted(disjunctiveRestrictions, removeInequalities)) {
        disjunctiveRestrictionToSparqlRestriction(disjunctiveRestrictions, subject, variablePrefixForDisjunction)
      } else {
        None
      }
    }).flatten
    // Step 5: Combine all disjunctions in a conjunction.
    val finalSparqlFilterRestriction = SparqlFilterRestriction.conjunction(disjunctionSparqlRestrictions)
    finalSparqlFilterRestriction.map(filter => CustomOperator(filter.toSparql))
  }

  private def shouldBeConverted(disjunctiveRestrictions: Seq[Option[DataSourceRestriction]],
                                removeInequalities: Boolean): Boolean = {
    disjunctiveRestrictions.forall(_.isDefined) && {
      !removeInequalities || disjunctiveRestrictions.flatten.forall(!_.isInstanceOf[DataSourceInequalityRestriction])
    }

  }

  private def disjunctiveRestrictionToSparqlRestriction(dataSourceRestrictions: Seq[Option[DataSourceRestriction]],
                                                        subject: String,
                                                        variablePrefix: String): Option[SparqlFilterRestriction] = {
    val sparqlFilterRestrictions = dataSourceRestrictions.flatten.zipWithIndex.
        map { case (dataSourceRestriction, restrictionIdx) =>
          val restrictionVariablePrefix = variablePrefix + restrictionIdx + "_"
          // Step 4a: convert to SPARQL filter restriction
          dataSourceRestriction.toSparqlFilter(subject, restrictionVariablePrefix)
        }
    // Step 4b: Combine all SPARQL filter restrictions as disjunction
    SparqlFilterRestriction.disjunction(sparqlFilterRestrictions)
  }

  private def convertDisjunctionToRestriction(disjunction: CnfBooleanOr,
                                              sourceOrTarget: Boolean): Seq[Option[DataSourceRestriction]] = {
    val dataSourceRestrictions = disjunction.leaves map {
      case cnfNot: CnfBooleanLeafNot =>
        convertComparison(cnfNot.booleanComparison.comparison, sourceOrTarget, invertRestriction = true)
      case CnfBooleanLeafComparison(comparisonOperator) =>
        convertComparison(comparisonOperator.comparison, sourceOrTarget, invertRestriction = false)
    }
    dataSourceRestrictions
  }

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
    ComparisonToRestrictionConverter.linkageRuleToRestriction(
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