package org.silkframework.runtime.plugin

import java.io.File
import java.net.{URL, URLClassLoader}
import java.util.ServiceLoader
import java.util.logging.Logger

import javax.inject.Inject
import org.silkframework.config.{Config, DefaultConfig, Prefixes}
import org.silkframework.runtime.resource.{EmptyResourceManager, ResourceManager}
import org.silkframework.runtime.serialization.Serialization
import org.silkframework.util.Identifier

import scala.collection.JavaConversions._
import scala.collection.immutable.ListMap
import scala.collection.mutable
import scala.reflect.ClassTag

/**
 * Registry of all available plugins.
 */
object PluginRegistry {
  @Inject
  private var configMgr: Config = DefaultConfig.instance

  private val log = Logger.getLogger(getClass.getName)

  final val PLUGIN_BLACKLIST_CONFIG_PATH = "plugin.blacklist"

  private def blacklistedPlugins: Set[Identifier] = {
    if(configMgr().hasPath(PLUGIN_BLACKLIST_CONFIG_PATH)) {
      configMgr().getString(PLUGIN_BLACKLIST_CONFIG_PATH)
          .split("\\s*,\\s*")
          .map(id => Identifier(id.trim))
          .toSet
    } else {
      Set.empty
    }
  }
  
  /** Map from plugin base types to an instance holding all plugins of that type.  */
  private var pluginTypes = Map[String, PluginType]()

  /** Map holding all plugins by their class name */
  private var plugins = Map[String, PluginDescription[_]]()

  /** Map holding all plugins by their ID. */
  private var pluginsById = Map[String, PluginDescription[_]]()

  /** All object plugin parameters. */
  private val objectPluginParameterClasses = new mutable.HashMap[Class[_], mutable.HashSet[Parameter]]

  // Register all plugins at instantiation of this singleton object.
  if(configMgr().hasPath("pluginRegistry.pluginFolder")) {
    registerJars(new File(configMgr().getString("pluginRegistry.pluginFolder")))
  } else {
    registerFromClasspath()
  }

  def pluginParameterPluginsValid(): Boolean =  {
    var pluginsWithoutXmlFormat: List[String] = Nil
    var pluginsWithoutJsonFormat: List[String] = Nil
    var pluginsNestingInvalid: List[String] = Nil
    for((pluginClass, usageInParameters) <- objectPluginParameterClasses) {
      if(Serialization.formatForMimeOption(pluginClass, "application/json").isEmpty) {
        pluginsWithoutJsonFormat ::= pluginClass.getCanonicalName
      }
      // TODO: XmlFormat not consistently used, e.g. DatasetSelection has methods in class instead.
//      if(Serialization.formatForMimeOption(pluginClass, "application/xml").isEmpty) {
//        pluginsWithoutXmlFormat ::= pluginClass.getCanonicalName
//      }
      checkInvalidObjectPluginParameterType(pluginClass, usageInParameters.toSeq) foreach { errorMessage =>
        pluginsNestingInvalid ::= errorMessage
      }
    }
    if(pluginsWithoutJsonFormat.nonEmpty || pluginsWithoutXmlFormat.nonEmpty || pluginsNestingInvalid.nonEmpty) {
      log.severe(s"Invalid plugin parameter classes found. Details: " + errorPart("Classes missing JSON format implementation", pluginsWithoutJsonFormat) +
          errorPart("Classes missing XML format implementation", pluginsWithoutXmlFormat) + errorPart("Other validation problems", pluginsNestingInvalid))
      false
    } else {
      true
    }
  }

  private def errorPart(errorType: String, errorDetails: List[String]): String = {
    if(errorDetails.isEmpty) {
      ""
    } else {
      s" $errorType: ${errorDetails.mkString(", ")}."
    }
  }

  // Returns an error message string if the object type is invalid.
  def checkInvalidObjectPluginParameterType(parameterType: Class[_],
                                            usageInParams: Seq[Parameter]): Option[String] = {
    var errorMessage = ""
    val needsCheck = usageInParams.exists(_.visibleInDialog)
    if(needsCheck) {
      for (param <- PluginDescription(parameterType).parameters if errorMessage.isEmpty && needsCheck) {
        if (param.parameterType.isInstanceOf[PluginObjectParameterTypeTrait]) {
          errorMessage = s"Found multiple nestings in object plugin parameter. Parameter '${param.label}' of parameter class " +
              s"'${parameterType.getSimpleName}' is itself a nested object parameter."
        }
      }
    }
    Some(errorMessage).filter(_.nonEmpty)
  }

  /**
   * Creates a new instance of a specific plugin.
   *
   * @param id The id of the plugin.
   * @param params The instantiation parameters.
   * @param resources The resource loader for retrieving referenced resources.
   * @tparam T The base type of the plugin.
   * @return A new instance of the plugin type with the given parameters.
   */
  def create[T: ClassTag](id: String, params: Map[String, String] = Map.empty)(implicit prefixes: Prefixes, resources: ResourceManager): T = {
    pluginType[T].create[T](id, params)
  }

  /**
   * Loads a plugin from the configuration.
   *
   * @param configPath The config path that contains the plugin parameters, e.g., workspace.plugin
   * @tparam T The type of the plugin.
   * @return The plugin instance.
   */
  def createFromConfig[T: ClassTag](configPath: String)(implicit prefixes: Prefixes = Prefixes.empty, resources: ResourceManager = EmptyResourceManager()): T = {
    createFromConfigOption[T](configPath) match {
      case Some(p) => p
      case None => throw new InvalidPluginException(s"Configuration property $configPath does not contain a plugin definition.")
    }
  }

  /**
    * Loads a plugin from the configuration.
    *
    * @param configPath The config path that contains the plugin parameters, e.g., workspace.plugin
    * @tparam T The type of the plugin.
    * @return The plugin instance, if the given config path is set.
    */
  def createFromConfigOption[T: ClassTag](configPath: String)
                                         (implicit prefixes: Prefixes = Prefixes.empty,
                                          resources: ResourceManager = EmptyResourceManager()): Option[T] = {
    if(!configMgr().hasPath(configPath + ".plugin")) {
      None
    } else {
      val config = configMgr().getConfig(configPath)
      // Retrieve plugin id
      val pluginId = config.getString("plugin")
      // Check if there are any configuration parameters available for this plugin
      val configValues = if(config.hasPath(pluginId)) config.getConfig(pluginId).entrySet().toSet else Set.empty
      // Instantiate plugin with configured parameters
      val pluginParams = for (entry <- configValues) yield (entry.getKey, entry.getValue.unwrapped().toString)
      val plugin = create[T](pluginId, pluginParams.toMap)
      log.fine(s"Loaded plugin $plugin")
      Some(plugin)
    }
  }

  /**
   * Given a plugin instance, extracts its plugin description and parameters.
   */
  def reflect(pluginInstance: AnyRef)(implicit prefixes: Prefixes): (PluginDescription[_], Map[String, String]) = {
    val desc = PluginDescription(pluginInstance.getClass)
    val parameters =
      for(param <- desc.parameters if param(pluginInstance) != null) yield
        (param.name, param.stringValue(pluginInstance))
    (desc, parameters.toMap)
  }

  /**
   * Returns a list of all available plugins of a specific type.
   */
  def availablePlugins[T: ClassTag]: Seq[PluginDescription[T]] = {
    val blackList = blacklistedPlugins
    pluginType[T]
        .availablePlugins.asInstanceOf[Seq[PluginDescription[T]]]
        .filterNot(p => blackList.contains(p.id))
        .sortBy(_.label)
  }

  /** Get a specific plugin description by plugin ID. */
  def pluginDescriptionById(pluginId: String): Option[PluginDescription[_]] = pluginsById.get(pluginId)

  /**
    * Returns a list of all available plugins of a specific runtime type.
    */
  def availablePluginsForClass(pluginClass: Class[_]): Seq[PluginDescription[_]] = {
    pluginTypes.get(pluginClass.getName) match {
      case Some(pluginType) => pluginType.availablePlugins
      case None => Seq.empty
    }
  }

  /**
   * Returns a map of all plugins grouped by category
   */
  def pluginsByCategory[T: ClassTag]: Map[String, Seq[PluginDescription[_]]] = {
    pluginType[T].pluginsByCategory
  }

  /**
    * Returns the plugin description of a single class.
    *
    * @return The plugin description or None, if this class has not been registered as a plugin.
    */
  def pluginDescription[T](pluginClass: Class[_]): Option[PluginDescription[T]] = {
    plugins.get(pluginClass.getName).map(_.asInstanceOf[PluginDescription[T]])
  }

  /**
   * Finds and registers all plugins in the classpath.
   */
  def registerFromClasspath(classLoader: ClassLoader = Thread.currentThread.getContextClassLoader): Unit = {
    // Load all plugin classes
    val loader = ServiceLoader.load(classOf[PluginModule], classLoader)
    val modules = loader.iterator().toList
    val pluginClasses = for(module <- modules; pluginClass <- module.pluginClasses) yield pluginClass

    // Create a plugin description for each plugin class (can be done in parallel)
    val pluginDescs = for(pluginClass <- pluginClasses.par) yield PluginDescription.create(pluginClass)

    // Register plugins (must currently be done sequentially as registerPlugin is not thread safe)
    for(pluginDesc <- pluginDescs.seq)
      registerPlugin(pluginDesc)

    // Load modules
    modules.foreach(_.load())
  }

  /**
   * Registers all plugins from a directory of jar files.
   * Also registers all plugins on the classpath.
   */
  def registerJars(jarDir: File) {
    //Collect all jar file in the specified directory
    val jarFiles = Option(jarDir.listFiles())
      .getOrElse(Array.empty)
      .filter(_.getName.endsWith(".jar"))

    //Load all found classes
    val jarClassLoader = URLClassLoader.newInstance(jarFiles.map(
      f => new URL("jar:file:" + f.getAbsolutePath + "!/")
    ), getClass.getClassLoader)

    val loader = ServiceLoader.load(classOf[PluginModule], jarClassLoader)
    val iter = loader.iterator()
    while(iter.hasNext) {
      val module = iter.next()
      module.pluginClasses.foreach(registerPlugin)
      module.load()
    }
  }

  // Checks if a plugin description is valid
  def checkPluginDescription(pluginDesc: PluginDescription[_]): Unit = {
    pluginDesc.parameters foreach { param =>
      if(!param.visibleInDialog && param.defaultValue.isEmpty) {
        throw new InvalidPluginException(s"Plugin '${pluginDesc.label}' is invalid. Parameter '${param.name}' must " +
            s"either be visible in a dialog or needs a default value.")
      }
    }
  }

  /**
    * Registers a single plugin.
    */
  def registerPlugin(pluginDesc: PluginDescription[_]): Unit = {
    checkPluginDescription(pluginDesc)
    if(!blacklistedPlugins.contains(pluginDesc.id)) {
      for (superType <- getSuperTypes(pluginDesc.pluginClass)) {
        val pluginType = pluginTypes.getOrElse(superType.getName, new PluginType)
        pluginTypes += ((superType.getName, pluginType))
        pluginType.register(pluginDesc)
      }
      plugins += ((pluginDesc.pluginClass.getName, pluginDesc))
      pluginsById += ((pluginDesc.id.toString, pluginDesc))
    }
    // Collect object parameter type in order to check them at the end of the initialization
    pluginDesc.parameters.foreach { param =>
      param.parameterType match {
        case paramType: PluginObjectParameterTypeTrait =>
          val parameters = objectPluginParameterClasses.getOrElseUpdate(paramType.pluginObjectParameterClass, mutable.HashSet.empty)
          parameters.add(param)
        case _ => // Do nothing
      }
    }
  }

  /**
   * Registers a single plugin.
   */
  def registerPlugin(implementingClass: Class[_]): Unit = {
    val pluginDesc = PluginDescription.create(implementingClass)
    registerPlugin(pluginDesc)
    log.fine(s"Loaded plugin " + pluginDesc.id)
  }

  private def getSuperTypes(clazz: Class[_]): Set[Class[_]] = {
    val superTypes = clazz.getInterfaces ++ Option(clazz.getSuperclass)
    val nonStdTypes = superTypes.filterNot(c => c.getName.startsWith("java") || c.getName.startsWith("scala"))
    nonStdTypes.toSet ++ nonStdTypes.flatMap(getSuperTypes)
  }

  private def pluginType[T: ClassTag]: PluginType = {
    val pluginClass = implicitly[ClassTag[T]].runtimeClass.getName
    pluginTypes.getOrElse(pluginClass, new PluginType)
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
     * @param resources The resource loader for retrieving referenced resources.
     * @tparam T The base type of the plugin.
     * @return A new instance of the plugin type with the given parameters.
     */
    def create[T: ClassTag](id: String, params: Map[String, String])(implicit prefixes: Prefixes, resources: ResourceManager): T = {
      val pluginClass = implicitly[ClassTag[T]].runtimeClass.getName
      val pluginDesc = plugins.getOrElse(id, throw new NoSuchElementException(s"No plugin '$id' found for class $pluginClass. Available plugins: ${plugins.keys.mkString(",")}"))
      pluginDesc(params).asInstanceOf[T]
    }

    /**
     * Registers a new plugin of this plugin type.
     */
    def register(pluginDesc: PluginDescription[_]): Unit = {
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
