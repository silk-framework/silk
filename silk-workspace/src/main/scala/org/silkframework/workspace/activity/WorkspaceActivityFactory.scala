package org.silkframework.workspace.activity

import org.silkframework.runtime.plugin.AnyPlugin

trait WorkspaceActivityFactory extends AnyPlugin {

  /**
    * Returns the type of generated activity.
    */
  def activityType: Class[_]

  /** True, if there is always exactly one instance of this activity */
  def isSingleton: Boolean = true

  /** Marks an activity as a cache activity, i.e. an activity that stores a cached value of something that is potentially
    * expensive to compute. */
  def isCacheActivity: Boolean = false

  /** True if this activity caches data that may be derived from a dataset. */
  def isDatasetRelatedCache: Boolean = false
}
