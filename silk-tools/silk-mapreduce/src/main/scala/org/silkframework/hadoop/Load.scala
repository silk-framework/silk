/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.silkframework.hadoop

import java.io.File
import java.util.logging.Logger

import org.apache.hadoop.fs.{FileSystem, Path}
import org.silkframework.config.{LinkSpecification, LinkingConfig}
import org.silkframework.execution.Loader
import org.silkframework.runtime.resource.FileResourceManager
import org.silkframework.util.DPair

/**
 * Populates the entity cache.
 */
class Load(silkConfigPath : String, entityCachePath : String, linkSpec : Option[String], hadoopConfig : org.apache.hadoop.conf.Configuration)
{
  private val logger = Logger.getLogger(getClass.getName)

  def apply()
  {
    Plugins.register()
    JenaPlugins.register()

    val config = loadConfig(new Path(silkConfigPath), new Path(entityCachePath))

    val linkSpecs = linkSpec match
    {
      case Some(spec) => config.linkSpec(spec) :: Nil
      case None => config.linkSpecs
    }

    for(linkSpec <- linkSpecs)
    {
      write(config, linkSpec, new Path(entityCachePath))
    }
  }

  private def loadConfig(filePath : Path, entityCachePath : Path) : LinkingConfig =
  {
    //Create two FileSystem objects, because the config file and the entity cache might be located in different file systems
    val configFS = FileSystem.get(filePath.toUri, hadoopConfig)
    val cacheFS = FileSystem.get(entityCachePath.toUri, hadoopConfig)

    //Copy the config file into the entity cache directory
    val inputStream = configFS.open(filePath)
    val outputStream = cacheFS.create(entityCachePath.suffix("/config.xml"))
    try
    {
      val buffer = new Array[Byte](4096)
      var c = inputStream.read(buffer)
      while(c != -1)
      {
        outputStream.write(buffer, 0, c)
        c = inputStream.read(buffer)
      }
    }
    finally
    {
      outputStream.close()
      inputStream.close()
    }

    //Load the configuration
    val stream = configFS.open(filePath)
    val resourceLoader = new FileResourceManager(new File(filePath.getParent.toUri))
    try
    {
      LinkingConfig.load(resourceLoader)(stream)
    }
    finally
    {
      stream.close()
    }
  }

  private def write(config : LinkingConfig, linkSpec : LinkSpecification, entityCachePath : Path)
  {
    val cacheFS = FileSystem.get(entityCachePath.toUri, hadoopConfig)

    val sources = linkSpec.dataSelections.map(_.sourceId).map(config.source(_))

    val entityDesc = linkSpec.entityDescriptions

    val caches = DPair(
      new HadoopEntityCache(entityDesc.source, linkSpec.rule.index(_), cacheFS, entityCachePath.suffix("/source/" + linkSpec.id + "/"), config.runtime),
      new HadoopEntityCache(entityDesc.target, linkSpec.rule.index(_), cacheFS, entityCachePath.suffix("/target/" + linkSpec.id + "/"), config.runtime)
    )

    new Loader(sources, caches)()
  }
}
