package org.silkframework.plugins.dataset.rdf.tasks.templating

import org.apache.jena.update.UpdateFactory
import org.apache.velocity.runtime.parser.node._
import org.silkframework.runtime.plugin.annotations.Plugin
import org.silkframework.runtime.templating.{CompiledTemplate, EvaluationConfig, TemplateEngine, TemplateVariableName, TemplateVariableValue}
import org.silkframework.runtime.validation.ValidationException

import java.io.{StringWriter, Writer}
import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

/**
  * A SPARQL Update templating engine based on Velocity.
  */
@Plugin(
  id = SparqlVelocityTemplateEngine.id,
  label = "SPARQL Velocity",
  description = "A SPARQL Update templating engine based on Apache Velocity."
)
case class SparqlVelocityTemplateEngine() extends TemplateEngine {

  override def compile(templateString: String): SparqlVelocityCompiledTemplate = {
    new SparqlVelocityCompiledTemplate(templateString)
  }
}

object SparqlVelocityTemplateEngine {
  final val id = "velocity"
}

/**
  * A compiled SPARQL Update template based on Velocity.
  */
class SparqlVelocityCompiledTemplate(val sparqlUpdateTemplate: String) extends CompiledTemplate {

  private val sparqlTemplate = SparqlVelocityTemplating.createTemplate(sparqlUpdateTemplate)

  override lazy val variables: Option[Seq[TemplateVariableName]] = {
    val rowVars = variableMethodUsages(SparqlVelocityTemplating.ROW_VAR_NAME)
      .map(u => new TemplateVariableName(u.parameterValue, ""))
    val inputPropVars = variableMethodUsages(SparqlVelocityTemplating.INPUT_PROPERTIES_VAR_NAME)
      .map(u => new TemplateVariableName(u.parameterValue, "inputProperties"))
    val outputPropVars = variableMethodUsages(SparqlVelocityTemplating.OUTPUT_PROPERTIES_VAR_NAME)
      .map(u => new TemplateVariableName(u.parameterValue, "outputProperties"))
    Some((rowVars ++ inputPropVars ++ outputPropVars).distinct)
  }

  override def evaluate(values: Map[String, AnyRef], writer: Writer): Unit = {
    // Separate entity variables from task property variables
    val entityVars = scala.collection.mutable.Map[String, String]()
    var inputProps = Map.empty[String, String]
    var outputProps = Map.empty[String, String]
    values.foreach {
      case ("inputProperties", m: java.util.Map[_, _]) =>
        inputProps = m.asScala.map { case (k, v) => String.valueOf(k) -> String.valueOf(v) }.toMap
      case ("outputProperties", m: java.util.Map[_, _]) =>
        outputProps = m.asScala.map { case (k, v) => String.valueOf(k) -> String.valueOf(v) }.toMap
      case (k, v) =>
        entityVars(k) = String.valueOf(v)
    }
    writer.write(SparqlVelocityTemplating.renderTemplate(sparqlTemplate, Row(entityVars.toMap), TaskProperties(inputProps, outputProps)))
  }

  override def evaluate(values: Seq[TemplateVariableValue], writer: Writer, evaluationConfig: EvaluationConfig): Unit = {
    evaluate(convertValues(values), writer)
  }

  // Extracts all method invocations on the given variable name in the config
  private[templating] def variableMethodUsages(variableName: String): Seq[TemplateVariableMethodUsage] = {
    sparqlTemplate.getData match {
      case simpleNode: SimpleNode =>
        retrieveRowMethodUsages(simpleNode, variableName)
      case None =>
        throw new RuntimeException(s"Unexpected error: Cannot retrieve $variableName object method usages from Velocity template.")
    }
  }

  override def usesRawUnsafe(): Boolean = {
    SparqlVelocityTemplating.templatingVariables.exists { variableName =>
      variableMethodUsages(variableName).exists(_.rowMethod == rawUnsafeMethodName)
    }
  }

  private val rawUnsafeMethodName = "rawUnsafe"

  private final val rowMethodsWithPathParameter = Set("uri", "plainLiteral", rawUnsafeMethodName, "exists")
  /** Retrieves the input paths that are used via the [[Row]] API. */
  private def retrieveRowMethodUsages(simpleNode: Node, varName: String): List[TemplateVariableMethodUsage] = {
    simpleNode match {
      case astMethod: ASTMethod =>
        astReferenceName(astMethod.jjtGetParent()) match {
          case Some(v) if v == varName &&
              rowMethodsWithPathParameter.contains(astMethod.getMethodName) &&
              validStringRowMethodParameter(astMethod) =>
            val parameterValue = astMethod.jjtGetChild(1).jjtGetChild(0).asInstanceOf[ASTStringLiteral].literal().stripPrefix("\"").stripSuffix("\"")
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

case class TemplateVariableMethodUsage(rowMethod: String, parameterValue: String)
