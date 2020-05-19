package org.silkframework.workspace.resources

class InMemoryResourceRepositoryTest extends PerProjectResourceRepositoryTest {

  override protected lazy val repository: ResourceRepository = InMemoryResourceRepository()
}
