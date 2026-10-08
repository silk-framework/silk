package controllers.projectApi

import controllers.util.{ItemLink, ItemType}
import org.silkframework.config.TaskSpec
import org.silkframework.rule.{TransformRule, TransformSpec}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.util.Identifier
import org.silkframework.workspace.changes.{ChangeHeader, RemoveVariable, ResourceDeleted, SetVariable}
import org.silkframework.workspace.{Project, ProjectTask}

/**
  * Links to where the current state behind a change is seen, built here so no client needs to know the routes:
  * a task change links the task page as long as the task exists (a removed task has none until the removal is
  * reverted), a mapping change the rule in the mapping editor instead while that rule exists, a variable or file
  * change the project page holding their widgets, an existing file its download and a workflow run its report.
  * Built from the header, which names the task, the rule, the file and the report the change concerns.
  */
object ChangeLinks {

  // The changes that link the project page alone: a deleted file has no download, whatever stands at its path now
  private val projectPageOnly = Set(classOf[SetVariable], classOf[RemoveVariable], classOf[ResourceDeleted]).map(_.getSimpleName)

  def of(project: Project, header: ChangeHeader)(implicit userContext: UserContext): Seq[ItemLink] = {
    val projectPage = ItemType.itemDetailsPage(ItemType.project, project.id, project.id)
    (header.taskId, header.path) match {
      case (Some(taskId), _) =>
        project.anyTaskOption(taskId).map(taskLink(project, _, header.ruleId)).toSeq ++ reportLink(project, taskId, header.executionId)
      case _ if projectPageOnly.contains(header.changeType) =>
        Seq(projectPage)
      case (None, Some(path)) =>
        projectPage +: downloadLink(project, path).toSeq
      case _ =>
        Seq.empty
    }
  }

  /** The page of the task, or for a mapping change the rule it concerns, while that rule exists. */
  private def taskLink(project: Project, task: ProjectTask[_ <: TaskSpec], ruleId: Option[Identifier]): ItemLink = {
    val page = ItemType.itemDetailsPage(ItemType.itemType(task.data), project.id, task.id)
    rule(task, ruleId).map(r => ItemLink("rule", s"Mapping rule '${r.labelOrId}'", s"${page.path}?ruleId=${r.id}")).getOrElse(page)
  }

  /** The rule a mapping change concerns, while it exists: the added or updated rule, the parent of a removal or reorder. */
  private def rule(task: ProjectTask[_ <: TaskSpec], ruleId: Option[Identifier]): Option[TransformRule] = {
    for {
      id <- ruleId
      transform <- Some(task.data).collect { case spec: TransformSpec => spec }
      (rule, _) <- transform.nestedRuleAndSourcePath(id.toString)
    } yield rule
  }

  /** The persisted execution report of a workflow run, whether or not the workflow still exists. */
  private def reportLink(project: Project, taskId: Identifier, executionId: Option[String]): Option[ItemLink] = {
    executionId.map { id =>
      val url = controllers.workspaceApi.routes.ReportsApi.retrieveReport(project.id, taskId.toString, id).url
      ItemLink("report", "Execution report", url, openInNewTab = true)
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
