package org.silkframework.dataset

import org.silkframework.runtime.resource.WritableResource

/**
  * Created on 7/13/16.
  */
trait WritableResourceDataset extends Dataset {
  def file: WritableResource

  def replaceWritableResource(writableResource: WritableResource): WritableResourceDataset
}
