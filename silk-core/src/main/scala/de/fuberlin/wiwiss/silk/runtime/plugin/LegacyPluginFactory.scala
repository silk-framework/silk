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

package de.fuberlin.wiwiss.silk.runtime.plugin

import java.io.File
import java.net.{URL, URLClassLoader}
import org.clapper.classutil._
import collection.immutable.ListMap
import de.fuberlin.wiwiss.silk.runtime.resource.{EmptyResourceManager, ResourceLoader}
import java.util.logging.{Level, Logger}

/**
 * An abstract Factory.
 */
@Deprecated
class LegacyPluginFactory[T : Manifest] {

  private val logger = Logger.getLogger(getClass.getName)

  /** Map of all plugins by their id. This is a list map as it preserves the iteration order of the entries. */
  private var plugins = ListMap[String, PluginDescription[T]]()

  /**
   * Creates a new instance of a specific plugin.
   */
  def apply(id: String, params: Map[String, String] = Map.empty, resourceLoader: ResourceLoader = EmptyResourceManager): T = {
    val plugin = {
      plugins.get(id) match {
        case Some(s) => s(params, resourceLoader)
        case None => throw new NoSuchElementException("No plugin called '" + id + "' found.")
      }
    }

    plugin
  }

  /**
   * Retrieves the parameters of a plugin instance e.g. to serialize it.
   */
  def unapply(t: T): Option[(PluginDescription[_], Map[String, String])] = {
    val desc = PluginDescription(getClass)
    val parameters = desc.parameters.map(param => (param.name, param(this).toString)).toMap
    Some((desc, parameters))
  }

  /**
   * Retrieves a specific plugin.
   */
  def plugin(id: String) = plugins(id)

  /**
   * List of all registered plugins.
   */
  def availablePlugins: Seq[PluginDescription[T]] = plugins.values.toSeq

  /**
   * A map from each category to all corresponding plugins
   */
  def pluginsByCategory: Map[String, Seq[PluginDescription[T]]] = {
    // Build a list of tuples of the form (category, plugin)
    val categoriesAndPlugins = for(plugin <- availablePlugins; category <- plugin.categories) yield (category, plugin)
    // Build a map from each category to all corresponding plugins
    categoriesAndPlugins.groupBy(_._1).mapValues(_.map(_._2))
  }

  /**
   * Registers a single plugin.
   */
  def register(implementationClass: Class[_ <: T]) {
    val pluginDesc = PluginDescription(implementationClass)
    plugins += ((pluginDesc.id, pluginDesc))
  }

  /**
   * Registers all plugins on the classpath.
   */
  def registerClasspath() {
    val classFinder = ClassFinder()
    val classes = classFinder.getClasses().toIterator

    val pluginClassNames = ClassFinder.concreteSubclasses(manifest[T].runtimeClass.getName, classes).map(_.name)
    val pluginClasses = pluginClassNames.map(Class.forName)

    for(pluginClass <- pluginClasses)
      register(pluginClass.asInstanceOf[Class[T]])
  }

  /**
   * Registers all plugins from a directory of jar files.
   */
  def registerJars(jarDir: File) {
    val pluginInterface = manifest[T].runtimeClass

    /** Test whether a specific class implements the plugin interface */
    def isPlugin(classInfo: ClassInfo) = {
      if(classInfo.implements(pluginInterface.getName) || classInfo.superClassName == pluginInterface.getName)
        true
      else {
        val superClass = getClass.getClassLoader.loadClass(classInfo.superClassName)
        val allSuperClasses = Stream.iterate[Class[_]](superClass)(_.getSuperclass)
                                    .takeWhile(_.getName != "java.lang.Object")
        allSuperClasses.exists(pluginInterface.isAssignableFrom)
      }
    }

    //Collect all jar file in the directory
    val jarFiles = Option(jarDir.listFiles())
        .getOrElse(throw new Exception("Directory " + jarDir + " does not exist"))
        .filter(_.getName.endsWith(".jar"))

    // Find the names of all classes that implement the plugin interface
    val jarClasses = ClassFinder(jarFiles).getClasses.toSeq
    val pluginClassNames = jarClasses.filter(isPlugin).map(_.name)

    //Load all found classes
    val jarClassLoader = URLClassLoader.newInstance(jarFiles.map(
      f => new URL("jar:file:" + f.getAbsolutePath + "!/")
    ), getClass.getClassLoader)

    val pluginClasses = pluginClassNames.map( (className:String) => {
      logger.log(Level.FINE, s"Loading class [ class :: $className ]")
      jarClassLoader.loadClass(className)
    })

    //Register all plugins
    for(pluginClass <- pluginClasses)
      register(pluginClass.asInstanceOf[Class[T]])
  }
}
