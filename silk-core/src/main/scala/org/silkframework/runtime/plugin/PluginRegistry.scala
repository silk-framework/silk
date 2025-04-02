package org.silkframework.runtime.plugin

import com.typesafe.config.{Config => TypesafeConfig}
import org.silkframework.config.{Config, ConfigValue, DefaultConfig}
import org.silkframework.util.Identifier

import java.io.File
import java.net.{URL, URLClassLoader}
import java.util.ServiceLoader
import java.util.logging.Logger
import javax.inject.Inject
import scala.collection.immutable.ListMap
import scala.collection.parallel.CollectionConverters.ImmutableSeqIsParallelizable
import scala.jdk.CollectionConverters.{IteratorHasAsScala, SetHasAsScala}
import scala.reflect.ClassTag

/**
 * Registry of all available plugins.
 */
object PluginRegistry {
  @Inject
  private var configMgr: Config = DefaultConfig.instance

  private val log = Logger.getLogger(getClass.getName)
  
  /** Map from plugin base types to an instance holding all plugins of that type.  */
  @volatile
  private var pluginTypes = Map[String, PluginTypeHolder]()

  /** Map holding all plugins by their class name */
  @volatile
  private var plugins = Map[String, PluginDescription[_]]()

  /** Map holding all plugins by their ID. */
  @volatile
  private var pluginsById = Map[String, Seq[PluginDescription[_]]]()

  @volatile
  private var timestamp: Long = System.currentTimeMillis()

  // Register all plugins at instantiation of this singleton object.
  Config.pluginFolder() match {
    case Some(folder) =>
      registerJars(folder)
    case None =>
      registerFromClasspath()
  }

  /**
    * Timestamp of the last update to the registry.
    */
  def lastUpdateTimestamp: Long = timestamp

  def allPlugins: Iterable[PluginDescription[_]] = {
    pluginsById.values.flatten
  }

  // Returns an error message string if the object type is invalid.
  def checkInvalidObjectPluginParameterType(parameterType: Class[_ <: AnyPlugin],
                                            usageInParams: Seq[PluginParameter]): Option[String] = {
    var errorMessage = ""
    val needsCheck = usageInParams.exists(_.visibleInDialog)
    if(needsCheck) {
      for (param <- ClassPluginDescription(parameterType).parameters if errorMessage.isEmpty && needsCheck) {
        if (param.parameterType.isInstanceOf[PluginObjectParameterTypeTrait]) {
          errorMessage = s"Found multiple nestings in object plugin parameter. Parameter '${param.label}' of parameter class " +
              s"'${parameterType.getSimpleName}' is itself a nested object parameter."
        }
      }
    }
    Some(errorMessage).filter(_.nonEmpty)
  }

  /**
   * Returns the plugin description of a specific plugin.
   */
  def pluginById[T: ClassTag](id: String): PluginDescription[T] = {
    pluginType[T].pluginById[T](id)
  }

  /**
   * Creates a new instance of a specific plugin.
   *
   * @param id The id of the plugin.
   * @param params The instantiation parameters.
   * @tparam T The base type of the plugin.
   * @return A new instance of the plugin type with the given parameters.
   */
  def create[T: ClassTag](id: String, params: ParameterValues = ParameterValues.empty)
                         (implicit context: PluginContext): T = {
    pluginType[T].create[T](id, params)
  }

  /**
   * Loads a plugin from the configuration.
   *
   * @param configPath The config path that contains the plugin parameters, e.g., workspace.plugin
   * @tparam T The type of the plugin.
   * @return The plugin instance.
   */
  def createFromConfig[T: ClassTag](configPath: String)
                                   (implicit context: PluginContext): T = {
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
                                         (implicit context: PluginContext): Option[T] = {
    if(!configMgr().hasPath(configPath + ".plugin")) {
      None
    } else {
      val config = configMgr().getConfig(configPath)
      // Retrieve plugin id
      val pluginId = config.getString("plugin")
      // Check if there are any configuration parameters available for this plugin
      val configValues = if(config.hasPath(pluginId)) config.getConfig(pluginId).entrySet().asScala else Set.empty
      // Instantiate plugin with configured parameters
      val pluginParams = for (entry <- configValues) yield (entry.getKey, entry.getValue.unwrapped().toString)
      val plugin = create[T](pluginId, ParameterValues.fromStringMap(pluginParams.toMap))
      log.fine(s"Loaded plugin $plugin")
      Some(plugin)
    }
  }

  /**
   * Given a plugin instance, extracts its plugin description and parameters.
   */
  def reflect(pluginInstance: AnyPlugin)(implicit pluginContext: PluginContext): (PluginDescription[_], Map[String, String]) = {
    val desc = ClassPluginDescription(pluginInstance.getClass)
    val parameters =
      for(param <- desc.parameters if param(pluginInstance) != null) yield
        (param.name, param.stringValue(pluginInstance))
    (desc, parameters.toMap)
  }

  /**
   * Returns a list of all available plugins of a specific type.
   */
  def availablePlugins[T: ClassTag]: Seq[PluginDescription[T]] = {
    val blackList = Config.blacklistedPlugins()
    pluginType[T]
        .availablePlugins.asInstanceOf[Seq[PluginDescription[T]]]
        .filterNot(p => blackList.contains(p.id))
        .sortBy(_.label)
  }

  /** Get a specific plugin description by plugin ID.
    *
    * @param pluginId     The ID of the plugin.
    * @param assignableTo Optional classes that the plugin must be assignable to.
    *                     Depending on 'forAllClassesAssignableTo' either all or at least one class must match.
    *                     Only matching plugins will be returned.
    * @param forAllClassesAssignableTo If this is true then all classes from the 'assignableTo' parameter must match, else
    *                                  only one needs to match.
    */
  def pluginDescriptionsById(pluginId: String,
                             assignableTo: Option[Seq[Class[_]]] = None,
                             forAllClassesAssignableTo: Boolean = false): Seq[PluginDescription[_]] = {
    pluginsById.get(pluginId).toSeq.flatten.filter( pluginDesc =>
      assignableTo
          .filter(_.nonEmpty)
          .forall ( assignableToClasses => if(forAllClassesAssignableTo) {
            assignableToClasses.forall(as => as.isAssignableFrom(pluginDesc.pluginClass))
          } else {
            assignableToClasses.exists(as => as.isAssignableFrom(pluginDesc.pluginClass))
          })
    )
  }

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
    val modules = loader.iterator().asScala.toList
    val pluginClasses = for(module <- modules; pluginClass <- module.pluginClasses) yield pluginClass

    // Create a plugin description for each plugin class (can be done in parallel)
    val pluginDescs = pluginClasses.par.map(PluginDescriptionFactory)

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
    if(!Config.blacklistedPlugins().contains(pluginDesc.id) && !(plugins.contains(pluginDesc.pluginClass.getName) && pluginsById.contains(pluginDesc.id))) {
      for (superType <- pluginDesc.pluginTypes) {
        val pluginType = pluginTypes.getOrElse(superType.name, new PluginTypeHolder)
        pluginTypes += ((superType.name, pluginType))
        pluginType.register(pluginDesc)
      }
      plugins += ((pluginDesc.pluginClass.getName, pluginDesc))
      pluginsById += ((pluginDesc.id.toString, pluginDesc :: (pluginsById.getOrElse(pluginDesc.id, Seq()).toList)))
      timestamp = System.currentTimeMillis()
    }
  }

  /**
   * Registers a single plugin.
   */
  def registerPlugin(implementingClass: Class[_ <: AnyPlugin]): Unit = {
    val pluginDesc = ClassPluginDescription.create(implementingClass)
    registerPlugin(pluginDesc)
    log.fine(s"Loaded plugin " + pluginDesc.id)
  }

  /**
    * Removes a plugin from the registry.
    */
  def unregisterPlugin(pluginDesc: PluginDescription[_]): Unit = {
    for  { superType <- pluginDesc.pluginTypes
           pluginType <- pluginTypes.get(superType.name)} {
      pluginType.unregister(pluginDesc.id)
    }
    plugins -= pluginDesc.pluginClass.getName

    // Remove plugin from pluginsById
    val existingPluginsForId = pluginsById.getOrElse(pluginDesc.id.toString, Seq.empty)
    val updatedPluginsForId = existingPluginsForId.filter(_.pluginClass.getName != pluginDesc.pluginClass.getName)
    if(updatedPluginsForId.nonEmpty) {
      pluginsById += ((pluginDesc.id.toString, updatedPluginsForId))
    } else {
      pluginsById -= pluginDesc.id.toString
    }

    timestamp = System.currentTimeMillis()
  }

  /**
    * Removes a plugin from the registry.
    */
  def unregisterPlugin(implementingClass: Class[_ <: AnyPlugin]): Unit = {
    unregisterPlugin(ClassPluginDescription.create(implementingClass))
  }

  private def pluginType[T: ClassTag]: PluginTypeHolder = {
    val pluginClass = implicitly[ClassTag[T]].runtimeClass
    pluginTypes.getOrElse(pluginClass.getName, new PluginTypeHolder)
  }

  /**
   * Holds all plugins that share a specific base type.
   */
  private class PluginTypeHolder {

    /** Map from plugin id to plugin description */
    @volatile
    private var plugins = ListMap[String, PluginDescription[_]]()

    def availablePlugins: Seq[PluginDescription[_]] = plugins.values.toSeq

    /**
     * Returns the plugin description of a specific plugin.
     */
    def pluginById[T: ClassTag](id: String): PluginDescription[T] = {
      val pluginClass = implicitly[ClassTag[T]].runtimeClass.getName
      plugins.getOrElse(id, throw new NoSuchElementException(s"No plugin '$id' found for class $pluginClass. Available plugins: ${plugins.keys.mkString(",")}"))
             .asInstanceOf[PluginDescription[T]]
    }

    /**
     * Creates a new instance of a specific plugin.
     *
     * @param id The id of the plugin.
     * @param params The instantiation parameters.
     * @tparam T The base type of the plugin.
     * @return A new instance of the plugin type with the given parameters.
     */
    def create[T: ClassTag](id: String, params: ParameterValues)
                           (implicit context: PluginContext): T = {
      pluginById[T](id).apply(params)
    }

    /**
     * Registers a new plugin of this plugin type.
     */
    def register(pluginDesc: PluginDescription[_]): Unit = {
      plugins += ((pluginDesc.id, pluginDesc))
    }

    /**
      * Removes a plugin from the registry.
      */
    def unregister(pluginId: Identifier): Unit = {
      plugins -= pluginId
    }

    /**
     * A map from each category to all corresponding plugins
     */
    def pluginsByCategory: Map[String, Seq[PluginDescription[_]]] = {
      // Build a list of tuples of the form (category, plugin)
      val categoriesAndPlugins = for(plugin <- availablePlugins; category <- plugin.categories) yield (category, plugin)
      // Build a map from each category to all corresponding plugins
      categoriesAndPlugins.groupBy(_._1).view.mapValues(_.map(_._2)).toMap
    }
  }

  object Config {

    private final val PLUGIN_FOLDER_CONFIG_PATH = "pluginRegistry.pluginFolder"

    private final val PLUGINS_CONFIG_PATH = "pluginRegistry.plugins"

    private final val LEGACY_PLUGIN_BLACKLIST_CONFIG_PATH = "plugin.blacklist"

    val pluginFolder: ConfigValue[Option[File]] = (config: TypesafeConfig) => {
      if(config.hasPath(PLUGIN_FOLDER_CONFIG_PATH)) {
        Some(new File(config.getString(PLUGIN_FOLDER_CONFIG_PATH)))
      } else {
        None
      }
    }

    val blacklistedPlugins: ConfigValue[Set[Identifier]] = (config: TypesafeConfig) => {
      var blacklist = Set[Identifier]()

      // Load blacklist from plugins config
      if(config.hasPath(PLUGINS_CONFIG_PATH)) {
        val allPluginsConf = config.getObject(PLUGINS_CONFIG_PATH)
        for(pluginId <- allPluginsConf.keySet().asScala) yield {
          val pluginConf = allPluginsConf.toConfig.getConfig(pluginId)
          if(pluginConf.hasPath("enabled") && !pluginConf.getBoolean("enabled")) {
            blacklist += pluginId
          }
        }
      }

      // Load legacy blacklist
      if(config.hasPath(LEGACY_PLUGIN_BLACKLIST_CONFIG_PATH)) {
        log.warning(s"Parameter '$LEGACY_PLUGIN_BLACKLIST_CONFIG_PATH' is deprecated. Please use $PLUGINS_CONFIG_PATH instead.")
        blacklist ++=
          config.getString(LEGACY_PLUGIN_BLACKLIST_CONFIG_PATH)
            .split("\\s*,\\s*")
            .map(id => Identifier(id.trim))
      }

      blacklist
    }

  }
}

/**
  * Function that creates a plugin description from a Java class.
  */
private object PluginDescriptionFactory extends (Class[_ <: AnyPlugin] => PluginDescription[_]) {
  override def apply(v1: Class[_ <: AnyPlugin]): PluginDescription[_] = {
    ClassPluginDescription.create(v1)
  }
}
