package org.silkframework.workspace

import java.util.concurrent.atomic.AtomicBoolean

import org.scalatest.{FlatSpec, MustMatchers}
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito._
import org.silkframework.config.{CustomTask, MetaData, PlainTask, Task, TaskSpec}
import org.silkframework.dataset.rdf.SparqlEndpoint
import org.silkframework.entity.EntitySchema
import org.silkframework.runtime.activity.{Activity, ActivityContext, TestUserContextTrait, UserContext}
import org.silkframework.runtime.plugin.PluginRegistry
import org.silkframework.runtime.resource.ResourceManager
import org.silkframework.runtime.validation.ServiceUnavailableException
import org.silkframework.util.{ConfigTestTrait, Identifier}
import org.silkframework.workspace.WorkspaceTest.{TestActivity, TestActivityFactory, TestTask, TestWorkspaceProvider}
import org.silkframework.workspace.activity.TaskActivityFactory
import org.silkframework.workspace.resources.InMemoryResourceRepository

import scala.collection.immutable.ListMap
import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.reflect.ClassTag
import scala.util.Try

class WorkspaceTest extends FlatSpec with MustMatchers with ConfigTestTrait with MockitoSugar with TestUserContextTrait {
  behavior of "Workspace"

  it should "throw a 503 if it is loading and reaching its timeout" in {
    val slowSeq = new Seq[ProjectConfig] {
      override def length: Int = 0
      override def apply(idx: Int): ProjectConfig = ProjectConfig(metaData = MetaData("project"))
      override def iterator: Iterator[ProjectConfig] = {
        Thread.sleep(5000)
        Iterator.empty
      }
    }
    val providerMock = mock[WorkspaceProvider]
    when(providerMock.readProjects()).thenReturn(slowSeq)
    val inMemoryResourceRepository = InMemoryResourceRepository()
    val workspace = new Workspace(providerMock, inMemoryResourceRepository)
    val started = new AtomicBoolean(false)
    Future {
      started.set(true)
      workspace.projects.headOption
    }
    while(!started.get()) {
      Thread.sleep(1)
    }
    Thread.sleep(1)
    intercept[ServiceUnavailableException] {
      workspace.projects.headOption
    }
  }

  it should "load caches after all projects have been loaded" in {
    val workspaceProvider = new TestWorkspaceProvider(loadTimePause = 1000)
    PluginRegistry.registerPlugin(classOf[TestActivityFactory])

    val project1 = Identifier("project1")
    val task1 = Identifier("task1")
    workspaceProvider.putProject(ProjectConfig(project1, metaData = MetaData(project1)))
    workspaceProvider.putTask(project1, PlainTask(task1,  TestTask()))

    val project2 = Identifier("project2")
    val task2 = Identifier("task2")
    workspaceProvider.putProject(ProjectConfig(project2, metaData = MetaData(project2)))
    workspaceProvider.putTask(project2, PlainTask(task2,  TestTask()))

    val workspace = new Workspace(workspaceProvider, InMemoryResourceRepository())

    // Make sure that all projects and tasks have been loaded
    workspace.projects.map(_.config.id) mustBe Seq(project1, project2)
    workspace.project(project1).allTasks.map(_.id) mustBe Seq(task1)
    workspace.project(project2).allTasks.map(_.id) mustBe Seq(task2)

    // Make sure that all projects and tasks have been loaded before starting any activity
    val projectLoadTimes = workspaceProvider.taskReadTimes.values
    val activityStartTimes = {
      for {project <- workspace.projects
           task <- project.tasks[TestTask]} yield {
        task.activity[TestActivity].control.startTime.getOrElse {
          fail("Autorun activity has not been started.")
        }
      }
    }

    projectLoadTimes.size mustBe 2
    activityStartTimes.size mustBe 2
    projectLoadTimes.max must be < activityStartTimes.min
  }

  override def propertyMap: Map[String, Option[String]] = Map(
    "workspace.timeouts.waitForWorkspaceInitialization" -> Some("1")
  )
}

object WorkspaceTest {

  /**
    * Workspace provider that keeps track on when projects and tasks have been loaded.
    *
    * @param loadTimePause After loading tasks, sleep for this many milliseconds.
    */
  class TestWorkspaceProvider(loadTimePause: Int) extends InMemoryWorkspaceProvider {

    // The timestamp when projects have been loaded the last time
    var projectTime: Long = 0

    // The timestamps when the tasks for each project have been loaded
    var taskReadTimes: ListMap[String, Long] = ListMap.empty

    override def readProjects()(implicit user: UserContext): Seq[ProjectConfig] = {
      projectTime = System.currentTimeMillis()
      super.readProjects()
    }

    override def readTasks[T <: TaskSpec : ClassTag](project: Identifier, projectResources: ResourceManager)
                                                    (implicit user: UserContext): Seq[Task[T]] = {
      val tasks = super.readTasks(project, projectResources)
      Thread.sleep(loadTimePause)
      taskReadTimes += ((project, System.currentTimeMillis()))
      tasks
    }
    override def readTasksSafe[T <: TaskSpec : ClassTag](project: Identifier, projectResources: ResourceManager)
                                                        (implicit user: UserContext): Seq[Either[Task[T], TaskLoadingError]] = {
      val tasks = super.readTasksSafe(project, projectResources)
      Thread.sleep(loadTimePause)
      taskReadTimes += ((project, System.currentTimeMillis()))
      tasks
    }
  }

  case class TestTask(testParam: String = "test value") extends CustomTask {
    override def inputSchemataOpt: Option[Seq[EntitySchema]] = None
    override def outputSchemaOpt: Option[EntitySchema] = None
  }

  case class TestActivityFactory() extends TaskActivityFactory[TestTask, TestActivity] {

    override def autoRun: Boolean = true

    def apply(task: ProjectTask[TestTask]): Activity[Unit] = {
      new TestActivity
    }
  }

  class TestActivity extends Activity[Unit] {
    override def run(context: ActivityContext[Unit])
                    (implicit userContext: UserContext): Unit = {
      // Does nothing
    }
  }

}
