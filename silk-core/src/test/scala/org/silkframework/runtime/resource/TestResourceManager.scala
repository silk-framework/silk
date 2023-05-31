package org.silkframework.runtime.resource

/** Resource manager that should be used in tests, so the EmptyResourceManager is not used directly, so its explicit
  * usages are more transparent and lucid. */
object TestResourceManager {
  def apply(): ResourceManager = EmptyResourceManager()
}