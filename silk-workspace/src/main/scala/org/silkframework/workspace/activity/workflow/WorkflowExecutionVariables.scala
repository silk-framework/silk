package org.silkframework.workspace.activity.workflow

import org.silkframework.config.TaskSpec
import org.silkframework.rule.RuleBlockSpec
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.templating.{TemplateVariable, TemplateVariableName, VariableScope}
import org.silkframework.util.Identifier
import org.silkframework.workspace.{Project, ProjectTask}

import scala.collection.mutable

/**
  * Determines the execution variables that a workflow run needs.
  *
  * Only the execution variables of the executed workflow seed a run. A variable that is referenced at execution
  * time from within the workflow's execution closure therefore has to be defined on the workflow itself, provided
  * when the run is started, or set by a 'Set execution variable' plugin before the referencing node runs, i.e. by a
  * node that precedes it in the workflow graph. Setters in parallel or disconnected branches run in no guaranteed order.
  * Execution variables defined on sub-tasks are ignored during a workflow run.
  * The analysis is exact for the variables that the tasks report as referenced.
  */
object WorkflowExecutionVariables {

  /**
    * The requirement of a single execution variable.
    *
    * @param name         The local name of the variable in the execution scope.
    * @param required     True, if the variable has to be provided when the run is started: some node references it without
    *                     a default on the workflow or a setter among its preceding nodes.
    * @param default      The default defined on the workflow itself, if any.
    * @param referencedBy Tasks whose templates reference the variable at execution time.
    * @param setBy        Tasks that set the variable during the run, regardless of where.
    */
  case class ExecutionVariableRequirement(name: String,
                                          required: Boolean,
                                          default: Option[TemplateVariable],
                                          referencedBy: Seq[ProjectTask[_ <: TaskSpec]],
                                          setBy: Seq[ProjectTask[_ <: TaskSpec]])

  /**
    * Analyses the execution closure of a workflow (its nodes, the rule blocks those use and sub-workflows,
    * recursively). Returns all execution variables that are referenced, set or defined on the workflow, sorted by name.
    */
  def apply(workflowTask: ProjectTask[Workflow], project: Project)
           (implicit userContext: UserContext): Seq[ExecutionVariableRequirement] = {
    val analysis = new Analysis(project)
    analysis.analyseWorkflow(workflowTask.data, setBefore = Set.empty)

    val defaults = workflowTask.executionVariables.map
    val names = (defaults.keys ++ analysis.referencedBy.keys ++ analysis.setBy.keys).toSeq.distinct.sorted
    for (name <- names) yield {
      ExecutionVariableRequirement(
        name = name,
        required = analysis.unsatisfied.contains(name) && !defaults.contains(name),
        default = defaults.get(name),
        referencedBy = analysis.referencedBy.get(name).map(_.toSeq).getOrElse(Seq.empty),
        setBy = analysis.setBy.get(name).map(_.toSeq).getOrElse(Seq.empty)
      )
    }
  }

  /**
    * Walks the nodes of a workflow in execution order and records, per execution variable, the tasks that reference
    * and set it, plus the variables that some node references without a setter among its preceding nodes.
    */
  private class Analysis(project: Project)(implicit userContext: UserContext) {

    val referencedBy = mutable.LinkedHashMap[String, mutable.LinkedHashSet[ProjectTask[_ <: TaskSpec]]]()
    val setBy = mutable.LinkedHashMap[String, mutable.LinkedHashSet[ProjectTask[_ <: TaskSpec]]]()
    val unsatisfied = mutable.Set[String]()

    /**
      * Analyses the nodes of a workflow. Returns the variables that its nodes set, which are set for the nodes after it.
      *
      * @param setBefore The variables that are set before the workflow runs.
      */
    def analyseWorkflow(workflow: Workflow, setBefore: Set[String]): Set[String] = {
      val setByNode = mutable.Map[String, Set[String]]()
      for (node <- workflow.topologicalSortedNodes; task <- project.anyTaskOption(node.task)) {
        val precedingNodes = workflow.dependencyNodesById(node.nodeId).precedingNodesRecursively
        val setBeforeNode = setBefore ++ precedingNodes.flatMap(preceding => setByNode.getOrElse(preceding.nodeId, Set.empty))
        val setByThisNode = task.data match {
          case subWorkflow: Workflow =>
            analyseWorkflow(subWorkflow, setBeforeNode)
          case _ =>
            analyseNode(executedWith(task), setBeforeNode)
        }
        setByNode.put(node.nodeId, setByThisNode)
      }
      setByNode.values.flatten.toSet
    }

    /** Records the variables that the tasks of a node reference and set. Returns the set ones. */
    private def analyseNode(tasks: Seq[ProjectTask[_ <: TaskSpec]], setBefore: Set[String]): Set[String] = {
      for (task <- tasks) {
        for (variable <- executionVariables(task.data.referencedVariables)) {
          referencedBy.getOrElseUpdate(variable, mutable.LinkedHashSet()).add(task)
          if (!setBefore.contains(variable)) {
            unsatisfied.add(variable)
          }
        }
        for (variable <- executionVariables(task.data.modifiedVariables)) {
          setBy.getOrElseUpdate(variable, mutable.LinkedHashSet()).add(task)
        }
      }
      tasks.flatMap(task => executionVariables(task.data.modifiedVariables)).toSet
    }

    /** The task of a node and the rule blocks its rules use, recursively. Data inputs and outputs do not run with the node. */
    private def executedWith(task: ProjectTask[_ <: TaskSpec]): Seq[ProjectTask[_ <: TaskSpec]] = {
      val visited = mutable.LinkedHashMap[Identifier, ProjectTask[_ <: TaskSpec]]()
      def visit(current: ProjectTask[_ <: TaskSpec]): Unit = {
        if (!visited.contains(current.id)) {
          visited.put(current.id, current)
          for (id <- current.data.referencedTasks; ruleBlock <- project.anyTaskOption(id) if ruleBlock.data.isInstanceOf[RuleBlockSpec]) {
            visit(ruleBlock)
          }
        }
      }
      visit(task)
      visited.values.toSeq
    }

    private def executionVariables(variables: Seq[TemplateVariableName]): Seq[String] = {
      variables.filter(_.scope == VariableScope.execution).map(_.name).distinct
    }
  }
}
