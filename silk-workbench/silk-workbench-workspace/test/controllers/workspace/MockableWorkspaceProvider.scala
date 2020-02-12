package controllers.workspace

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.annotations.Plugin
import org.silkframework.runtime.resource.ResourceManager
import org.silkframework.util.Identifier
import org.silkframework.workspace.{InMemoryWorkspaceProvider, ProjectConfig, TaskLoadingError}

import scala.reflect.ClassTag
import scala.util.Try

@Plugin(
  id = "mockableInMemoryWorkspace",
  label = "Mockable In-memory workspace provider",
  description = "An in-memory test workspace provider that can be configured to mock specific methods at runtime."
)
class MockableWorkspaceProvider extends InMemoryWorkspaceProvider {
  val id: UUID = UUID.randomUUID()

  MockableWorkspaceProvider.configWorkspace(id, new BreakableWorkspaceProviderConfig())

  private def config: BreakableWorkspaceProviderConfig = MockableWorkspaceProvider.workspaceConfig(id)

  override def readProjects()(implicit userContext: UserContext): Seq[ProjectConfig] = {
    config.readProjects().getOrElse(
      super.readProjects()
    )
  }

  override def readTasks[T <: TaskSpec : ClassTag](project: Identifier, projectResources: ResourceManager)(implicit userContext: UserContext): Seq[Task[T]] = {
    config.readTasks[T](project, projectResources).getOrElse(
      super.readTasks[T](project, projectResources)
    )
  }

  override def readTasksSafe[T <: TaskSpec : ClassTag](project: Identifier,
                                                       projectResources: ResourceManager)
                                                      (implicit user: UserContext): Seq[Either[Task[T], TaskLoadingError]] = {
    config.readTasksSafe[T](project, projectResources).getOrElse(
      super.readTasksSafe[T](project, projectResources)
    )
  }
}

object MockableWorkspaceProvider {
  private val workspaceConfig = new ConcurrentHashMap[UUID, BreakableWorkspaceProviderConfig]

  def configWorkspace(workspaceId: UUID, breakableWorkspaceProviderConfig: BreakableWorkspaceProviderConfig): Unit = {
    workspaceConfig.put(workspaceId, breakableWorkspaceProviderConfig)
  }

  def workspaceConfig(workspaceId: UUID): BreakableWorkspaceProviderConfig = {
    workspaceConfig.get(workspaceId)
  }
}

/**
  * Calls the given call-backs before the actual methods are called.
  * If the result is None, the actual method is called, else the result is directly returned.
  *
  * @param readProjects  Replaces the readProjects method of the workspace provider.
  * @param readTasks     Replaces the readTasks method of the workspace provider.
  * @param readTasksSafe Replaces the readTasksSafe method of the workspace provider.
  */
class BreakableWorkspaceProviderConfig {
  def readProjects()
                  (implicit user: UserContext): Option[Seq[ProjectConfig]] = None

  def readTasks[T <: TaskSpec : ClassTag](project: Identifier, projectResources: ResourceManager)
                                         (implicit user: UserContext): Option[Seq[Task[T]]] = None

  def readTasksSafe[T <: TaskSpec : ClassTag](project: Identifier, projectResources: ResourceManager)
                                             (implicit user: UserContext): Option[Seq[Either[Task[T], TaskLoadingError]]] = None
}