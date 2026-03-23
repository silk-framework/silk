package org.silkframework.plugins.dataset.rdf.tasks.templating

import org.apache.velocity.VelocityContext
import org.apache.velocity.runtime.parser.node._
import org.silkframework.runtime.plugin.annotations.Plugin
import org.silkframework.runtime.templating.{CompiledTemplate, EvaluationConfig, TemplateEngine, TemplateMethodUsage, TemplateVariableName, TemplateVariableValue}

import java.io.Writer

/**
  * A general-purpose templating engine based on Apache Velocity.
  */
@Plugin(
  id = VelocityTemplateEngine.id,
  label = "Velocity Engine",
  description = "A templating engine based on Apache Velocity."
)
case class VelocityTemplateEngine() extends TemplateEngine {

  override def compile(templateString: String): VelocityCompiledTemplate = {
    new VelocityCompiledTemplate(templateString)
  }
}

object VelocityTemplateEngine {
  final val id = "velocity"
}

/**
  * A compiled template based on Apache Velocity.
  */
class VelocityCompiledTemplate(val templateString: String) extends CompiledTemplate {

  private val velocityTemplate = SparqlVelocityTemplating.createTemplate(templateString)

  override lazy val variables: Option[Seq[TemplateVariableName]] = {
    Some(extractVariableReferences(velocityTemplate.getData.asInstanceOf[SimpleNode])
      .map(name => new TemplateVariableName(name, "")).distinct)
  }

  override def evaluate(values: Map[String, AnyRef], writer: Writer): Unit = {
    val context = new VelocityContext()
    values.foreach { case (k, v) => context.put(k, v) }
    writer.write(SparqlVelocityTemplating.renderTemplate(velocityTemplate, context))
  }

  override def evaluate(values: Seq[TemplateVariableValue], writer: Writer, evaluationConfig: EvaluationConfig): Unit = {
    evaluate(convertValues(values), writer)
  }

  /** Extracts top-level variable references from the Velocity AST. */
  private def extractVariableReferences(node: Node): List[String] = {
    node match {
      case ref: ASTReference =>
        List(ref.getRootString)
      case other: SimpleNode =>
        (0 until other.jjtGetNumChildren()).flatMap(idx => extractVariableReferences(other.jjtGetChild(idx))).toList
      case _ =>
        List.empty
    }
  }

  override def methodUsages(variableName: String): Seq[TemplateMethodUsage] = {
    velocityTemplate.getData match {
      case simpleNode: SimpleNode =>
        retrieveMethodUsages(simpleNode, variableName)
      case None =>
        throw new RuntimeException(s"Unexpected error: Cannot retrieve $variableName object method usages from Velocity template.")
    }
  }

  /** Retrieves method usages on a given variable from the AST. */
  private def retrieveMethodUsages(simpleNode: Node, varName: String): List[TemplateMethodUsage] = {
    simpleNode match {
      case astMethod: ASTMethod =>
        astReferenceName(astMethod.jjtGetParent()) match {
          case Some(v) if v == varName && hasSingleStringParameter(astMethod) =>
            val parameterValue = astMethod.jjtGetChild(1).jjtGetChild(0).asInstanceOf[ASTStringLiteral].literal().stripPrefix("\"").stripSuffix("\"")
            List(TemplateMethodUsage(astMethod.getMethodName, parameterValue))
          case _ =>
            List.empty
        }
      case other: SimpleNode =>
        retrieveChildMethodUsages(other, varName)
    }
  }

  /** Checks that there is a single string constant as parameter. */
  private def hasSingleStringParameter(astMethod: ASTMethod): Boolean = {
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

  private def retrieveChildMethodUsages(other: SimpleNode, varName: String): List[TemplateMethodUsage] = {
    val childPaths = for (idx <- 0 until other.jjtGetNumChildren()) yield {
      retrieveMethodUsages(other.jjtGetChild(idx), varName)
    }
    childPaths.fold(List.empty[TemplateMethodUsage])((a, b) => a ::: b)
  }
}
