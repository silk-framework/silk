package org.silkframework.plugins.dataset.rdf.tasks.templating
import org.apache.jena.update.UpdateFactory
import org.apache.velocity.runtime.parser.node._
import org.silkframework.entity.EntitySchema
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.execution.local.EmptyEntityTable
import org.silkframework.plugins.dataset.rdf.sparql.{Row, SparqlTemplating}
import org.silkframework.runtime.validation.ValidationException

import scala.util.{Failure, Success, Try}

/**
  * A SPARQL Update templating engine based on Velocity.
  */
case class SparqlTemplatingEngineVelocity(sparqlUpdateTemplate: String, batchSize: Int) extends SparqlUpdateTemplatingEngine {
  private val sparqlTemplate = SparqlTemplating.createTemplate(sparqlUpdateTemplate)

  override def generate(placeholderAssignments: Map[String, String]): String = {
    SparqlTemplating.renderTemplate(sparqlTemplate, Row(placeholderAssignments))
  }

  override def validate(): Unit = {
    // We cannot generate meaningful example values for the template if $row.asRawUnsafe() is used, because it could generate arbitrary SPARQL syntax.
    if(!usesAsRawUnsafe()) {
      val assignments = inputPaths().map(p => (p, "urn:generic:1")).toMap // Valid URI string is valid in URI and literal position, so use always the same URI
      val sparqlQuery = Try(generate(assignments)) match {
        case Failure(exception) =>
          throw new ValidationException("The SPARQL Update template could not be rendered with example value. Error message: " + exception.getMessage)
        case Success(value) => value
      }
      Try(UpdateFactory.create(sparqlQuery)).failed.toOption foreach { parseError =>
        throw new ValidationException("The SPARQL Update template does not generate valid SPARQL Update queries. Error message: " +
            parseError.getMessage + ", example query: " + sparqlQuery)
      }
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
    rowMethodUsages().map(_.parameter).distinct
  }

  private def rowMethodUsages(): Seq[RowMethodUsage] = {
    sparqlTemplate.getData match {
      case simpleNode: SimpleNode =>
        // This should always be the case
        retrieveRowMethodUsages(simpleNode)
      case None =>
        throw new RuntimeException("Unexpected error: Cannot retrieve row object method usages from Velocity template.")
    }
  }

  private def usesAsRawUnsafe(): Boolean = {
    rowMethodUsages().exists(_.rowMethod == asRawUnsafeMethodName)
  }

  private val asRawUnsafeMethodName = "asRawUnsafe"

  final val rowMethodsWithPathParameter = Set("asUri", "asPlainLiteral", asRawUnsafeMethodName, "exists")
  /** Retrieves the input paths that are used via the [[Row]] API. */
  private def retrieveRowMethodUsages(simpleNode: Node): List[RowMethodUsage] = {
    simpleNode match {
      case astMethod: ASTMethod =>
        astReferenceName(astMethod.jjtGetParent()) match {
          case Some("row") if rowMethodsWithPathParameter.contains(astMethod.getMethodName) && validStringRowMethodParameter(astMethod) =>
            // Collect parameter values from the specified methods of the 'row' object, since only these must all be input paths.
            val parameterValue = astMethod.jjtGetChild(1).jjtGetChild(0).asInstanceOf[ASTStringLiteral].literal()
            List(RowMethodUsage(astMethod.getMethodName, parameterValue))
          case _ =>
            List.empty
        }
      case other: SimpleNode =>
        retrieveChildRowMethodUsages(other)
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

  private case class RowMethodUsage(rowMethod: String, parameter: String)

  private def astReferenceName(node: Node): Option[String] = {
    node match {
      case reference: ASTReference =>
        Some(reference.getRootString)
      case _ =>
        None
    }
  }

  private def retrieveChildRowMethodUsages(other: SimpleNode): List[RowMethodUsage] = {
    val childPaths = for (idx <- 0 until other.jjtGetNumChildren()) yield {
      retrieveRowMethodUsages(other.jjtGetChild(idx))
    }
    childPaths.fold(List.empty[RowMethodUsage])((a, b) => a ::: b)
  }
}
