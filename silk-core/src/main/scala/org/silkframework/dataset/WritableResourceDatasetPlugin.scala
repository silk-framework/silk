package org.silkframework.dataset

import org.silkframework.runtime.resource.WritableResource

/**
  * Created on 7/13/16.
  */
trait WritableResourceDatasetPlugin extends DatasetPlugin {
  def file: WritableResource

  def replaceWritableResource(writableResource: WritableResource): WritableResourceDatasetPlugin
}
