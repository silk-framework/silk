package org.silkframework.workspace

import java.util.concurrent.atomic.AtomicBoolean

import org.scalatest.FlatSpec
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito._
import org.silkframework.runtime.activity.TestUserContextTrait
import org.silkframework.runtime.validation.ServiceUnavailableException
import org.silkframework.util.ConfigTestTrait
import org.silkframework.workspace.resources.InMemoryResourceRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class WorkspaceTest extends FlatSpec with ConfigTestTrait with MockitoSugar with TestUserContextTrait {
  behavior of "Workspace"

  it should "throw a 503 if it is loading and reaching its timeout" in {
    val slowSeq = new Seq[ProjectConfig] {
      override def length: Int = 0
      override def apply(idx: Int): ProjectConfig = ProjectConfig()
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

  override def propertyMap: Map[String, Option[String]] = Map(
    "workspace.timeouts.waitForWorkspaceInitialization" -> Some("1")
  )
}
