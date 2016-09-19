package org.silkframework.workspace

import org.scalatest.{FlatSpec, ShouldMatchers}
import org.silkframework.config.LinkSpec
import org.silkframework.dataset.Dataset
import org.silkframework.entity.Path
import org.silkframework.plugins.dataset.InternalDataset
import org.silkframework.plugins.distance.characterbased.QGramsMetric
import org.silkframework.rule.LinkageRule
import org.silkframework.rule.input.PathInput
import org.silkframework.rule.similarity.Comparison
import org.silkframework.runtime.resource.ResourceNotFoundException

/**
  * Created on 9/13/16.
  */
trait WorkspaceProviderTestTrait extends FlatSpec with ShouldMatchers {
  val PROJECTNAME = "ProjectName"
  val PROJECTNAMEOTHER = "ProjectNameOther"
  val CHILD = "child"

  def createWorkspaceProvider(): WorkspaceProvider

  private val workspace = createWorkspaceProvider()

  val rule =
    LinkageRule(
      operator = Some(
        Comparison(
          id = "compareNames",
          threshold = 0.1,
          metric = QGramsMetric(q = 3),
          inputs = PathInput("foafName", Path("http://xmlns.com/foaf/0.1/name")) ::
              PathInput("foafName2", Path("http://xmlns.com/foaf/0.1/name")) :: Nil
        )
      ),
      linkType = "http://www.w3.org/2002/07/owl#sameAs"
    )

  val dataset = InternalDataset()

  val linkTask = LinkSpec(rule = rule)

  it should "read and write projects" in {
    val project = createProject(PROJECTNAME)
    val project2 = createProject(PROJECTNAMEOTHER)
    workspace.readProjects().find(_.id == project.id) should be (Some(project.copy(projectResourceUriOpt = Some(project.generateDefaultUri))))
    workspace.readProjects().find(_.id == project2.id) should be (Some(project2.copy(projectResourceUriOpt = Some(project2.generateDefaultUri))))
  }

  private def createProject(projectName: String): ProjectConfig = {
    val project = ProjectConfig(projectName)
    workspace.putProject(project)
    project
  }

  it should "read and write linking tasks" in {
    workspace.putTask(PROJECTNAME, "task", linkTask)
    workspace.readTasks[LinkSpec](PROJECTNAME).headOption.map(_._2) should be (Some(linkTask))
  }

  it should "read and write dataset tasks" in {
    workspace.putTask(PROJECTNAME, "dataset", dataset)
    workspace.readTasks[Dataset](PROJECTNAME).headOption.map(_._2) should be (Some(dataset))
  }

  it should "delete linking tasks" in {
    workspace.readTasks[LinkSpec](PROJECTNAME).headOption shouldBe defined
    workspace.deleteTask[LinkSpec](PROJECTNAME, "task")
    workspace.readTasks[LinkSpec](PROJECTNAME).headOption shouldBe empty
  }

  it should "delete dataset tasks" in {
    workspace.readTasks[Dataset](PROJECTNAME).headOption shouldBe defined
    workspace.deleteTask[Dataset](PROJECTNAME, "dataset")
    workspace.readTasks[Dataset](PROJECTNAME).headOption shouldBe empty
  }

  it should "delete projects" in {
    workspace.readProjects().size shouldBe 2
    workspace.deleteProject(PROJECTNAME)
    workspace.readProjects().size shouldBe 1
  }

  it should "manage project resources separately and correctly" in {
    workspace.readProjects().size shouldBe 1
    createProject(PROJECTNAME)
    workspace.readProjects().size shouldBe 2
    val res1 = workspace.projectResources(PROJECTNAME)
    val res2 = workspace.projectResources(PROJECTNAMEOTHER)
    res1 should not be theSameInstanceAs (res2)
    val child1 = res1.get(CHILD)
    child1.write("content")
    intercept[ResourceNotFoundException] {
      res2.get(CHILD, mustExist = true)
    }
    val child1Other = res2.get(CHILD)
    child1 should not be theSameInstanceAs (child1Other)
    val res1Again = workspace.projectResources(PROJECTNAME)
    res1Again should be theSameInstanceAs (res1)
  }
}
