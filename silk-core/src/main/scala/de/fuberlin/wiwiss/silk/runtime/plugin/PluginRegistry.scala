package de.fuberlin.wiwiss.silk.runtime.plugin

import java.util.ServiceLoader
import de.fuberlin.wiwiss.silk.runtime.resource.{EmptyResourceManager, ResourceLoader}
import scala.collection.immutable.ListMap
import scala.reflect.ClassTag

/**
 * Registry of all available plugins.
 */
object PluginRegistry {

  /** Map from plugin base types to an instance holding all plugins of that type.  */
  private var pluginTypes = Map[String, PluginType]()

  // Register all plugins at instantiation of this singleton object.
  registerAll()

  /**
   * Creates a new instance of a specific plugin.
   *
   * @param id The id of the plugin.
   * @param params The instantiation parameters.
   * @param resourceLoader The resource loader for retrieving referenced resources.
   * @tparam T The based type of the plugin.
   * @return A new instance of the plugin type with the given parameters.
   */
  def create[T: ClassTag](id: String, params: Map[String, String] = Map.empty, resourceLoader: ResourceLoader = EmptyResourceManager): T = {
    pluginType[T].create[T](id, params, resourceLoader)
  }

  def reflect(pluginInstance: AnyRef): (PluginDescription[_], Map[String, String]) = {
    val desc = PluginDescription(pluginInstance.getClass)
    val parameters = desc.parameters.map(param => (param.name, param(pluginInstance).toString)).toMap
    (desc, parameters)
  }

  def availablePlugins[T: ClassTag] = {
    pluginType[T].availablePlugins
  }

  def pluginsByCategoty[T: ClassTag] = {
    pluginType[T].pluginsByCategory
  }

  /**
   * Finds and registers all plugins in the classpath.
   */
  //TODO also register plugins from jars in plugin directory
  def registerAll(): Unit = {
    val loader = ServiceLoader.load(classOf[PluginModule])
    val iter = loader.iterator()
    while(iter.hasNext) {
      iter.next().pluginClasses.foreach(registerPlugin)
    }
  }

  /**
   * Registers a single plugin.
   */
  def registerPlugin(implementingClass: Class[_]): Unit = {
    for(superType <- getSuperTypes(implementingClass)) {
      val pluginType = pluginTypes.getOrElse(superType.getName, new PluginType)
      pluginTypes += ((superType.getName, pluginType))
      pluginType.register(implementingClass)
    }
  }

  private def getSuperTypes(clazz: Class[_]): Set[Class[_]] = {
    val superTypes = clazz.getInterfaces ++ Option(clazz.getSuperclass)
    val nonStdTypes = superTypes.filterNot(c => c.getName.startsWith("java") || c.getName.startsWith("scala"))
    nonStdTypes.toSet ++ nonStdTypes.flatMap(getSuperTypes)
  }

  private def pluginType[T: ClassTag] = {
    val pluginClass = implicitly[ClassTag[T]].runtimeClass.getName
    pluginTypes.getOrElse(pluginClass, throw new NoSuchElementException(s"No plugins for type '$pluginClass' registered."))
  }

  /**
   * Holds all plugins that share a specific base type.
   */
  private class PluginType {

    /** Map from plugin id to plugin description */
    private var plugins = ListMap[String, PluginDescription[_]]()

    def availablePlugins: Seq[PluginDescription[_]] = plugins.values.toSeq

    /**
     * Creates a new instance of a specific plugin.
     *
     * @param id The id of the plugin.
     * @param params The instantiation parameters.
     * @param resourceLoader The resource loader for retrieving referenced resources.
     * @tparam T The based type of the plugin.
     * @return A new instance of the plugin type with the given parameters.
     */
    def create[T: ClassTag](id: String, params: Map[String, String], resourceLoader: ResourceLoader): T = {
      val pluginClass = implicitly[ClassTag[T]].runtimeClass.getName
      val pluginDesc = plugins.getOrElse(id, throw new NoSuchElementException(s"No plugin '$id' found for class $pluginClass. Available plugins: ${plugins.keys.mkString(",")}"))
      pluginDesc(params, resourceLoader).asInstanceOf[T]
    }

    /**
     * Registers a new plugin of this plugin type.
     */
    def register(pluginClass: Class[_]): Unit = {
      val pluginDesc = PluginDescription(pluginClass)
      plugins += ((pluginDesc.id, pluginDesc))
    }

    /**
     * A map from each category to all corresponding plugins
     */
    def pluginsByCategory: Map[String, Seq[PluginDescription[_]]] = {
      // Build a list of tuples of the form (category, plugin)
      val categoriesAndPlugins = for(plugin <- availablePlugins; category <- plugin.categories) yield (category, plugin)
      // Build a map from each category to all corresponding plugins
      categoriesAndPlugins.groupBy(_._1).mapValues(_.map(_._2))
    }
    
  }
  
}
