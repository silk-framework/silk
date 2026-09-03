package controllers.projectApi

import controllers.util.{ItemLink, ItemType}
import org.silkframework.config.TaskSpec
import org.silkframework.rule.TransformSpec
import org.silkframework.runtime.activity.UserContext
import org.silkframework.workspace.changes._
import org.silkframework.workspace.{Project, ProjectTask}

/**
  * Links to where the current state behind a change is seen, built here so no client needs to know the routes:
  * a task change links the task page as long as the task exists (a removed task has none until the removal is
  * reverted), a mapping change the rule in the mapping editor instead while that rule exists, a variable or file
  * change the project page holding their widgets, an existing file its download and a workflow run its report.
  */
object ChangeLinks {

  def of(project: Project, change: Change)(implicit userContext: UserContext): Seq[ItemLink] = {
    val projectPage = ItemType.itemDetailsPage(ItemType.project, project.id, project.id)
    change match {
      case names: NamesTask =>
        project.anyTaskOption(names.taskId).map(taskLink(project, _, names)).toSeq ++ reportLink(project, names)
      case _: SetVariable | _: RemoveVariable | _: ResourceDeleted =>
        Seq(projectPage)
      case ResourceCreated(path, _) =>
        projectPage +: downloadLink(project, path).toSeq
      case ResourceOverwritten(path, _, _) =>
        projectPage +: downloadLink(project, path).toSeq
      case _ =>
        Seq.empty
    }
  }

  /** The page of the task, or for a mapping change the rule it concerns, while that rule exists. */
  private def taskLink(project: Project, task: ProjectTask[_ <: TaskSpec], change: NamesTask): ItemLink = {
    val page = ItemType.itemDetailsPage(ItemType.itemType(task.data), project.id, task.id)
    // An addition or update links the rule itself, a removal or reorder the parent it happened in
    val ruleId = change match {
      case added: AddMapping => Some(added.rule.id)
      case updated: UpdateMapping => Some(updated.after.id)
      case removed: RemoveMapping => Some(removed.parentId)
      case reordered: ReorderMappings => Some(reordered.parentId)
      case _ => None
    }
    val rule = for {
      id <- ruleId
      transform <- Some(task.data).collect { case spec: TransformSpec => spec }
      (rule, _) <- transform.nestedRuleAndSourcePath(id.toString)
    } yield ItemLink("rule", s"Mapping rule '${rule.labelOrId}'", s"${page.path}?ruleId=$id")
    rule.getOrElse(page)
  }

  /** The persisted execution report of a workflow run, whether or not the workflow still exists. */
  private def reportLink(project: Project, change: NamesTask): Option[ItemLink] = {
    change match {
      case WorkflowExecuted(taskId, Some(executionId), _, _) =>
        val url = controllers.workspaceApi.routes.ReportsApi.retrieveReport(project.id, taskId.toString, executionId).url
        Some(ItemLink("report", "Execution report", url, openInNewTab = true))
      case _ =>
        None
    }
  }

  /** The download of a file, while it exists. */
  private def downloadLink(project: Project, path: String)(implicit userContext: UserContext): Option[ItemLink] = {
    if(project.resources.getInPath(path).exists) {
      val url = controllers.workspace.routes.ResourceApi.getFileForDownload(project.id, path).url
      Some(ItemLink("download", "Download file", url, openInNewTab = true))
    } else {
      None
    }
  }
}
