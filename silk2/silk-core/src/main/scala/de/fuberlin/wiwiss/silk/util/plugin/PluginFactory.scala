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

package de.fuberlin.wiwiss.silk.util.plugin

import java.io.File
import java.net.{URL, URLClassLoader}
import org.clapper.classutil._
import collection.immutable.ListMap

/**
 * An abstract Factory.
 */
class PluginFactory[T <: AnyPlugin : Manifest] {

  /** Map of all plugins by their id. This is a list map as it preserves the iteration order of the entries. */
  private var plugins = ListMap[String, PluginDescription[T]]()

  /**
   * Creates a new instance of a specific plugin.
   */
  def apply(id: String, params: Map[String, String] = Map.empty, resourceLoader: ResourceLoader = new EmptyResourceManager): T = {
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
  def unapply(t: T): Option[(String, Map[String, String])] = {
    Some(t.id, t.parameters)
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
    val classes = classFinder.getClasses()

    val pluginClassNames = ClassFinder.concreteSubclasses(manifest[T].runtimeClass.getName, classes).map(_.name)
    val pluginClasses = pluginClassNames.map(Class.forName)

    for(pluginClass <- pluginClasses)
      register(pluginClass.asInstanceOf[Class[T]])
  }

  /**
   * Registers all plugins which from a directory of jar files.
   */
  def registerJars(jarDir: File) {
    //Collect all jar file in the directory
    val jarFiles = Option(jarDir.listFiles())
                   .getOrElse(throw new Exception("Directory " + jarDir + " does not exist"))
                   .filter(_.getName.endsWith(".jar"))

    //Create a classinfo of the plugin interface
    val pluginClassInfo = new ClassInfo {
      val name: String = manifest[T].runtimeClass.getName
      def signature: String = null
      def methods: Set[MethodInfo] = null
      def location: File = null
      def superClassName: String = null
      val modifiers: Set[Modifier.Modifier] = Set(Modifier.Interface)
      def fields: Set[FieldInfo] = null
      def interfaces: List[String] = null
    }

    ///Find the names of all classes which implement the plugin interface
    val classes = ClassFinder(jarFiles).getClasses ++ Iterator(pluginClassInfo)
    val pluginClassNames = ClassFinder.concreteSubclasses(manifest[T].runtimeClass.getName, classes).map(_.name)

    //Load all found classes
    val classLoader = URLClassLoader.newInstance(jarFiles.map(file => new URL("jar:file:" + file.getAbsolutePath + "!/")), getClass.getClassLoader)
    val pluginClasses = pluginClassNames.map(classLoader.loadClass)

    //Register all plugins
    for(pluginClass <- pluginClasses)
      register(pluginClass.asInstanceOf[Class[T]])
  }
}
