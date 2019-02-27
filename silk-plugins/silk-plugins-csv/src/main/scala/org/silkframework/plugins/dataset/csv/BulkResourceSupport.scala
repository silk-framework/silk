package org.silkframework.plugins.dataset.csv

import java.io._
import java.util.logging.Logger

import org.silkframework.runtime.resource.{Resource, WritableResource}


trait BulkResourceSupport {

  private val log: Logger = Logger.getLogger(this.getClass.getSimpleName)

  def checkResource(resource: WritableResource): Resource = {
    if (resource.name.endsWith(".zip") && !new File(resource.path).isDirectory) {
      log info "Zipped Resource found."
      BulkResource(resource)
    }
    else if (new File(resource.path).isDirectory) {
      log info "Resource Folder found."
      resource
    }
    else{
      resource
    }
  }

}

