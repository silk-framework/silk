package org.silkframework.plugins.dataset.rdf.tasks.templating
import org.apache.velocity.runtime.parser.node.{ASTIdentifier, ASTMethod, ASTReference, Node, SimpleNode}
import org.silkframework.entity.EntitySchema
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.execution.local.EmptyEntityTable
import org.silkframework.plugins.dataset.rdf.sparql.{Row, SparqlTemplating}

/**
  * A SPARQL Update templating engine based on Velocity.
  */
case class SparqlTemplatingEngineVelocity(sparqlUpdateTemplate: String, batchSize: Int) extends SparqlUpdateTemplatingEngine {
  private val sparqlTemplate = SparqlTemplating.createTemplate(sparqlUpdateTemplate)

  override def generate(placeholderAssignments: Map[String, String]): String = {
    SparqlTemplating.renderTemplate(sparqlTemplate, Row(placeholderAssignments))
  }

  override def validate(): Unit = { /*TODO: Add validation*/}

  override def inputSchema: EntitySchema = {
    val properties = inputPaths()
    if (properties.isEmpty) {
      EmptyEntityTable.schema // Static template, no input data needed
    } else {
      EntitySchema("", properties.map(p => UntypedPath(p).asUntypedValueType).toIndexedSeq)
    }
  }

  def inputPaths(): Seq[String] = {
    sparqlTemplate.getData match {
      case simpleNode: SimpleNode =>
        // This should always be the case
        retrievePaths(simpleNode)
      case None =>
        throw new RuntimeException("Unexpected error: Cannot retrieve paths from Velocity template.")
    }
  }

  final val rowMethodsWithPathParameter = Set("asUri", "asPlainLiteral", "asRawUnsafe", "exists")
  /** Retrieves the input paths that are used via the [[Row]] API. */
  private def retrievePaths(simpleNode: Node): List[String] = {
    simpleNode match {
      case astMethod: ASTMethod =>
        astReferenceName(astMethod.jjtGetParent()) match {
          case Some("row") if rowMethodsWithPathParameter.contains(astMethod.getMethodName) && astMethod.jjtGetNumChildren() == 2 =>
            // Collect parameter values from the specified methods of the 'row' object, since only these must all be input paths.
            val parameter = astMethod.jjtGetChild(1)
            List(parameter.literal())
          case None =>
            List.empty
        }
      case other: SimpleNode =>
        retrieveChildPaths(other)
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

  private def retrieveChildPaths(other: SimpleNode): List[String] = {
    val childPaths = for (idx <- 0 until other.jjtGetNumChildren()) yield {
      retrievePaths(other.jjtGetChild(idx))
    }
    childPaths.fold(List.empty[String])((a, b) => a ::: b)
  }
}
