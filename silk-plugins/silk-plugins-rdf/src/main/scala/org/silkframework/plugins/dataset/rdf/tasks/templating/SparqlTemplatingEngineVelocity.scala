package org.silkframework.plugins.dataset.rdf.tasks.templating
import org.apache.jena.update.UpdateFactory
import org.apache.velocity.runtime.parser.node._
import org.silkframework.entity.EntitySchema
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.execution.local.EmptyEntityTable
import org.silkframework.runtime.validation.ValidationException

import scala.util.{Failure, Success, Try}

/**
  * A SPARQL Update templating engine based on Velocity.
  */
case class SparqlTemplatingEngineVelocity(sparqlUpdateTemplate: String, batchSize: Int) extends SparqlUpdateTemplatingEngine {
  private val sparqlTemplate = SparqlVelocityTemplating.createTemplate(sparqlUpdateTemplate)

  override def generate(placeholderAssignments: Map[String, String], taskProperties: TaskProperties): String = {
    SparqlVelocityTemplating.renderTemplate(sparqlTemplate, Row(placeholderAssignments), taskProperties)
  }

  override def validate(): Unit = {
    // We cannot generate meaningful example values for the template if $row.asRawUnsafe() is used, because it could generate arbitrary SPARQL syntax.
    if(!usesAsRawUnsafe()) {
      // Generate example input assignments
      val genericUri = "urn:generic:1" // Valid URI string is valid in URI and literal position, so use always the same URI
      val assignments = inputPaths().map(p => (p, genericUri)).toMap
      val inputPropertiesAssignments = variableMethodUsages(SparqlVelocityTemplating.INPUT_PROPERTIES_VAR_NAME).map(_.parameterValue -> genericUri).toMap
      val outputPropertiesAssignments = variableMethodUsages(SparqlVelocityTemplating.OUTPUT_PROPERTIES_VAR_NAME).map(_.parameterValue -> genericUri).toMap
      // Generate SPARQL Update query with example assignments
      val sparqlQuery = Try(generate(assignments, TaskProperties(inputPropertiesAssignments, outputPropertiesAssignments))) match {
        case Failure(exception) =>
          throw new ValidationException("The SPARQL Update template could not be rendered with example value. Error message: " + exception.getMessage, exception)
        case Success(value) => value
      }
      // Validate generated SPARQL Update query
      Try(UpdateFactory.create(sparqlQuery)).failed.toOption foreach { parseError =>
        throw new ValidationException("The SPARQL Update template does not generate valid SPARQL Update queries. Error message: " +
            parseError.getMessage + ", example query: " + sparqlQuery)
      }
      // If queries should be batched, also check if queries can be batched, i.e. concatenated and still have valid syntax
      if (batchSize > 1) {
        val batchSparql = sparqlQuery + "\n" + sparqlQuery
        Try(UpdateFactory.create(batchSparql)).failed.toOption foreach { parseError =>
          throw new ValidationException("The SPARQL Update template cannot be batched processed. There is probably a ';' missing at the end. Error message: " +
              parseError.getMessage + ", example batch query: " + batchSparql)
        }
      }
    }
  }

  override def inputSchema: EntitySchema = {
    val properties = inputPaths()
    if (properties.isEmpty) {
      EmptyEntityTable.schema // Static template, no input data needed
    } else {
      EntitySchema("", properties.map(p => UntypedPath(p).asUntypedValueType).toIndexedSeq)
    }
  }

  def inputPaths(): Seq[String] = {
    variableMethodUsages(SparqlVelocityTemplating.ROW_VAR_NAME).map(_.parameterValue).distinct
  }

  // Extracts all method invocations on the given variable name in the config
  def variableMethodUsages(variableName: String): Seq[TemplateVariableMethodUsage] = {
    sparqlTemplate.getData match {
      case simpleNode: SimpleNode =>
        // This should always be the case
        retrieveRowMethodUsages(simpleNode, variableName)
      case None =>
        throw new RuntimeException(s"Unexpected error: Cannot retrieve $variableName object method usages from Velocity template.")
    }
  }

  private def usesAsRawUnsafe(): Boolean = {
    SparqlVelocityTemplating.templatingVariables.exists { variableName =>
      variableMethodUsages(variableName).exists(_.rowMethod == asRawUnsafeMethodName)
    }
  }

  private val asRawUnsafeMethodName = "asRawUnsafe"

  final val rowMethodsWithPathParameter = Set("asUri", "asPlainLiteral", asRawUnsafeMethodName, "exists")
  /** Retrieves the input paths that are used via the [[Row]] API. */
  private def retrieveRowMethodUsages(simpleNode: Node, varName: String): List[TemplateVariableMethodUsage] = {
    simpleNode match {
      case astMethod: ASTMethod =>
        astReferenceName(astMethod.jjtGetParent()) match {
          case Some(v) if v == varName &&
              rowMethodsWithPathParameter.contains(astMethod.getMethodName) &&
              validStringRowMethodParameter(astMethod) =>
            // Collect parameter values from the specified methods of the 'row' object, since only these must all be input paths.
            val parameterValue = astMethod.jjtGetChild(1).jjtGetChild(0).asInstanceOf[ASTStringLiteral].literal()
            List(TemplateVariableMethodUsage(astMethod.getMethodName, parameterValue))
          case _ =>
            List.empty
        }
      case other: SimpleNode =>
        retrieveChildRowMethodUsages(other, varName)
    }
  }

  // Make sure that there is a single string constant as parameter
  private def validStringRowMethodParameter(astMethod: ASTMethod): Boolean = {
    astMethod.jjtGetNumChildren() == 2 && {
      val parameter = astMethod.jjtGetChild(1)
      parameter.isInstanceOf[ASTExpression] &&
        parameter.jjtGetNumChildren() == 1 &&
        parameter.jjtGetChild(0).isInstanceOf[ASTStringLiteral] &&
        parameter.jjtGetChild(0).asInstanceOf[ASTStringLiteral].isConstant
    }
  }

  case class TemplateVariableMethodUsage(rowMethod: String, parameterValue: String)

  private def astReferenceName(node: Node): Option[String] = {
    node match {
      case reference: ASTReference =>
        Some(reference.getRootString)
      case _ =>
        None
    }
  }

  private def retrieveChildRowMethodUsages(other: SimpleNode, varName: String): List[TemplateVariableMethodUsage] = {
    val childPaths = for (idx <- 0 until other.jjtGetNumChildren()) yield {
      retrieveRowMethodUsages(other.jjtGetChild(idx), varName)
    }
    childPaths.fold(List.empty[TemplateVariableMethodUsage])((a, b) => a ::: b)
  }
}
