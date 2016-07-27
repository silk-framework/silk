package org.silkframework.workspace.activity.workflow

import org.silkframework.config.TaskSpec
import org.silkframework.dataset.{Dataset, DatasetTask, VariableDataset}
import org.silkframework.util.Identifier
import org.silkframework.workspace.{ProjectTask, Project}

import scala.xml.Node

case class Workflow(id: Identifier, operators: Seq[WorkflowOperator], datasets: Seq[WorkflowDataset]) extends TaskSpec {

  def nodes: Seq[WorkflowNode] = operators ++ datasets

  def node(name: String) = {
    nodes.find(_.nodeId == name)
        .getOrElse(throw new NoSuchElementException(s"Cannot find node $name in the workflow."))
  }

  def toXML = {
    <Workflow id={id.toString}>
      {for (op <- operators) yield {
        <Operator
        posX={op.position._1.toString}
        posY={op.position._2.toString}
        task={op.task}
        inputs={op.inputs.mkString(",")}
        outputs={op.outputs.mkString(",")}
        errorOutputs={op.errorOutputs.mkString(",")}
        id={op.nodeId}/>
    }}{for (ds <- datasets) yield {
        <Dataset
        posX={ds.position._1.toString}
        posY={ds.position._2.toString}
        task={ds.task}
        inputs={ds.inputs.mkString(",")}
        outputs={ds.outputs.mkString(",")}
        id={ds.nodeId}/>
    }}
    </Workflow>
  }

  /**
    * Returns a topologically sorted sequence of [[WorkflowOperator]] used in this workflow.
    */
  def topologicalSortedOperators(project: Project): Seq[WorkflowOperator] = {
    val inputs = inputDatasets(project)
    val outputs = outputDatasets(project)
    val pureInputDatasets = inputs.map(_.id.toString).toSet -- outputs.map(_.id.toString)
    var done = pureInputDatasets
    var sortedOperators = Vector.empty[WorkflowOperator]
    var operatorsToSort = operators
    while (operatorsToSort.size > 0) {
      val (satisfied, unsatisfied) = operatorsToSort.partition(op => op.inputs.forall(done))
      if (satisfied.size == 0) {
        throw new RuntimeException("Cannot topologically sort operators in workflow " + id.toString + "!")
      }
      sortedOperators ++= satisfied
      done ++= satisfied.map(_.nodeId)
      operatorsToSort = unsatisfied
      // Add datasets to 'done' that are not written to by the remaining operators
      val satisfiedDatasets = outputs.filter(ds => operatorsToSort.forall(op => !op.outputs.contains(ds.id.toString)))
      done ++= satisfiedDatasets.map(_.id.toString)
    }
    sortedOperators
  }

  /**
    * Returns all variable datasets and how they are used in the workflow.
    *
    * @param project
    * @return
    * @throws Exception if a variable dataset is used as input and output, which is not allowed.
    */
  def variableDatasets(project: Project): AllVariableDatasets = {
    val variableDatasetsUsedInOutput =
      for (datasetTask <- outputDatasets(project)
           if datasetTask.data.isInstanceOf[VariableDataset]) yield {
        datasetTask.id.toString
      }

    val variableDatasetsUsedInInput =
      for (datasetTask <- inputDatasets(project)
           if datasetTask.data.isInstanceOf[VariableDataset]) yield {
        datasetTask.id.toString
      }
    val bothInAndOut = variableDatasetsUsedInInput.toSet & variableDatasetsUsedInOutput.toSet
    if (bothInAndOut.size > 0) {
      throw new scala.Exception("Cannot use variable dataset as input AND output! Affected datasets: " + bothInAndOut.mkString(", "))
    }
    AllVariableDatasets(variableDatasetsUsedInInput, variableDatasetsUsedInOutput)
  }

  def inputDatasets(project: Project): Seq[ProjectTask[Dataset]] = {
    for (datasetId <- operators.flatMap(_.inputs).distinct;
         dataset <- project.taskOption[Dataset](datasetId)) yield {
      dataset
    }
  }

  def outputDatasets(project: Project): Seq[ProjectTask[Dataset]] = {
    for (datasetId <- operators.flatMap(_.outputs).distinct;
         dataset <- project.taskOption[Dataset](datasetId)) yield {
      dataset
    }
  }

  case class AllVariableDatasets(dataSources: Seq[String], sinks: Seq[String])

}

object Workflow {

  def fromXML(xml: Node) = {
    val id = (xml \ "@id").text

    val operators =
      for (op <- xml \ "Operator") yield {
        val inputStr = (op \ "@inputs").text
        val outputStr = (op \ "@outputs").text
        val errorOutputStr = (op \ "@errorOutputs").text
        val task = (op \ "@task").text
        WorkflowOperator(
          inputs = if (inputStr.isEmpty) Seq.empty else inputStr.split(',').toSeq,
          task = task,
          outputs = if (outputStr.isEmpty) Seq.empty else outputStr.split(',').toSeq,
          errorOutputs = if (errorOutputStr.trim.isEmpty) Seq() else errorOutputStr.split(',').toSeq,
          position = ((op \ "@posX").text.toInt, (op \ "@posY").text.toInt),
          nodeId = {
            val node = ((op \ "@id"))
            if (node.isEmpty) {
              task
            } else {
              node.text
            }
          }
        )
      }

    val datasets =
      for (ds <- xml \ "Dataset") yield {
        val inputStr = (ds \ "@inputs").text
        val outputStr = (ds \ "@outputs").text
        val task = (ds \ "@task").text
        WorkflowDataset(
          inputs = if (inputStr.isEmpty) Seq.empty else inputStr.split(',').toSeq,
          task = task,
          outputs = if (outputStr.isEmpty) Seq.empty else outputStr.split(',').toSeq,
          position = ((ds \ "@posX").text.toInt, (ds \ "@posY").text.toInt),
          nodeId = {
            val node = ((ds \ "@id"))
            if (node.isEmpty) {
              task
            } else {
              node.text
            }
          }
        )
      }

    new Workflow(if (id.nonEmpty) Identifier(id) else Identifier.random, operators, datasets)
  }
}