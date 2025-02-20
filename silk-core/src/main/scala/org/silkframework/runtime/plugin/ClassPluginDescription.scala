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

package org.silkframework.runtime.plugin

import org.silkframework.runtime.plugin.annotations.{Action, Param, Plugin}
import org.silkframework.runtime.plugin.types.EnumerationParameterType
import org.silkframework.runtime.resource.ResourceNotFoundException
import org.silkframework.util.Identifier
import org.silkframework.util.StringUtils._
import org.silkframework.workspace.WorkspaceReadTrait

import java.lang.reflect.{Constructor, InvocationTargetException, Method, ParameterizedType, Type}
import java.net.URLConnection
import java.nio.charset.StandardCharsets
import java.util.Base64
import scala.io.{Codec, Source}
import scala.language.existentials

/**
  * Describes a plugin that is based on a Scala class.
  *
  * @param id The id of this plugin.
  * @param categories The categories to which this plugin belongs to.
  * @param label A human-readable label.
  * @param description A short (few sentence) description of this plugin.
  * @param documentation Documentation for this plugin in Markdown.
  * @param parameters The parameters of the plugin class.
  * @param constructor The constructor for creating a new instance of this plugin.
  * @param pluginTypes The plugin types for this plugin. Ideally just one.
  * @param icon The plugin icon as Data URL string. If the string is empty, a generic icon is used.
  * @tparam T The class that implements this plugin.
  */
class ClassPluginDescription[+T <: AnyPlugin](val id: Identifier,
                                              val categories: Seq[String],
                                              val label: String,
                                              val description: String,
                                              val documentation: String,
                                              val parameters: Seq[ClassPluginParameter],
                                              constructor: Constructor[T],
                                              val pluginTypes: Seq[PluginTypeDescription],
                                              val icon: Option[String],
                                              val actions: Seq[PluginAction]) extends PluginDescription[T] {

  /**
    * The plugin class.
    */
  def pluginClass: Class[_ <: T] = {
    constructor.getDeclaringClass
  }

  /**
   * Creates a new instance of this plugin.
   *
   * @param parameterValues The parameter values to be used for instantiation. Will override any default.
   * @param ignoreNonExistingParameters If true, parameter values for parameters that do not exist are ignored.
    *                                   If false, creation will fail if a parameter is provided that does not exist on the plugin.
   */
  def apply(parameterValues: ParameterValues = ParameterValues.empty,
            ignoreNonExistingParameters: Boolean = true)
           (implicit context: PluginContext): T = {
    if (!ignoreNonExistingParameters) {
      validateParameters(parameterValues)
    }
    val parsedParameters = parseParameters(parameterValues)
    try {
      val plugin = constructor.newInstance(parsedParameters: _*)
      plugin.init(this, parameterValues.templates)
      plugin
    } catch {
      case ex: InvocationTargetException => throw ex.getCause
    }
  }

  override def toString: String = label

  /**
    * Throws an exception if a parameter value is provided that does not exist on this plugin.
    */
  private def validateParameters(parameterValues: ParameterValues): Unit = {
    val invalidParameters = parameterValues.values.keySet -- parameters.map(_.name)
    if (invalidParameters.nonEmpty) {
      throw new InvalidPluginParameterValueException(s"The following parameters cannot be set on plugin '$label' because they are no valid parameters:" +
        s" ${invalidParameters.mkString(", ")}. Valid parameters are: ${parameters.map(_.name).mkString(", ")}")
    }
  }

}

/**
 * Factory for plugin description.
 */
object ClassPluginDescription {

  /**
    * Returns a plugin description for a given class.
    * If available, returns an already registered plugin description.
    */
  def apply[T <: AnyPlugin](pluginClass: Class[T]): PluginDescription[T] = {
    PluginRegistry.pluginDescription(pluginClass).getOrElse(create(pluginClass))
  }

  /**
    * Creates a new plugin description from a class.
    */
  def create[T <: AnyPlugin](pluginClass: Class[T]): ClassPluginDescription[T] = {
    getAnnotation(pluginClass) match {
      case Some(annotation) => createFromAnnotation(pluginClass, annotation)
      case None => createFromClass(pluginClass)
    }
  }

  private def getAnnotation[T](pluginClass: Class[T]): Option[Plugin] = {
    Option(pluginClass.getAnnotation(classOf[Plugin]))
  }

  private def createFromAnnotation[T <: AnyPlugin](pluginClass: Class[T], annotation: Plugin): ClassPluginDescription[T] = {
    val pluginTypes = getPluginTypes(pluginClass)

    // Generate documentation
    val docBuilder = new StringBuilder()
    docBuilder ++= loadMarkdownDocumentation(pluginClass, annotation.documentationFile)
    if (docBuilder.nonEmpty) {
      docBuilder ++= "\n"
    }
    for {
      pluginType <- pluginTypes
      customDescription <- pluginType.customDescription.generate(pluginClass)
    } {
      customDescription.generateDocumentation(docBuilder)
    }

    new ClassPluginDescription(
      id = annotation.id,
      label = annotation.label,
      categories = annotation.categories,
      description = annotation.description.stripMargin,
      documentation = docBuilder.toString,
      parameters = getParameters(pluginClass),
      constructor = getConstructor(pluginClass),
      pluginTypes = pluginTypes,
      icon = loadIcon(pluginClass, annotation.iconFile()),
      actions = getActions(pluginClass)
    )
  }

  private def createFromClass[T <: AnyPlugin](pluginClass: Class[T]): ClassPluginDescription[T] = {
    try {
      new ClassPluginDescription(
        id = Identifier.fromAllowed(pluginClass.getSimpleName),
        label = pluginClass.getSimpleName,
        categories = Seq(PluginCategories.uncategorized),
        description = "",
        documentation = "",
        parameters = getParameters(pluginClass),
        constructor = getConstructor(pluginClass),
        pluginTypes = getPluginTypes(pluginClass),
        icon = None,
        actions = Seq.empty
      )
    } catch {
      case ex: InvalidPluginException =>
        throw new InvalidPluginException(s"Cannot extract plugin description for plugin class '${pluginClass.getCanonicalName}'. Details: ${ex.getMessage}", ex)
    }
  }

  private def loadMarkdownDocumentation(pluginClass: Class[_], fileName: String): String = {
    if(fileName.trim.isEmpty) {
      ""
    } else {
      loadFileIntoString(pluginClass, fileName)
    }
  }

  private def loadIcon(pluginClass: Class[_], fileName: String): Option[String] = {
    if(fileName.isEmpty) {
      None
    } else {
      val data = loadFileIntoString(pluginClass, fileName)
      val dataBase64 = Base64.getEncoder.encodeToString(data.getBytes(StandardCharsets.UTF_8))
      val mimeType = URLConnection.guessContentTypeFromName(fileName)
      Some(s"data:$mimeType;base64,$dataBase64")
    }
  }

  /**
   * Loads a file from the classpath into a string.
   */
  private def loadFileIntoString(pluginClass: Class[_], fileName: String): String = {
    val inputStream = pluginClass.getResourceAsStream(fileName)
    if (inputStream == null) {
      throw new ResourceNotFoundException(s"The file for plugin $pluginClass has not been found at '$fileName'.")
    }
    val source = Source.fromInputStream(inputStream)(Codec.UTF8)
    try {
      source.getLines.mkString("\n")
    } finally {
      source.close()
    }
  }

  private def getConstructor[T](pluginClass: Class[T]): Constructor[T] = {
    pluginClass.getConstructors.toList match {
      case constructor :: _ => constructor.asInstanceOf[Constructor[T]]
      case Nil => throw new InvalidPluginException("Plugin " + pluginClass.getName + " does not provide a constructor")
    }
  }

  private def getParameters[T](pluginClass: Class[T]): Array[ClassPluginParameter] = {
    val constructor = getConstructor(pluginClass)
    val paramAnnotations = constructor.getParameterAnnotations.map(_.collect{ case p: Param => p })
    val parameterNames = constructor.getParameters.map(_.getName)
    val parameterTypes = constructor.getGenericParameterTypes
    val defaultValues = getDefaultValues(pluginClass, parameterNames.length)

    for ((((parName, parType), defaultValue), annotations) <- parameterNames zip parameterTypes zip defaultValues zip paramAnnotations) yield {
      val pluginParam = annotations.headOption

      val label = pluginParam match {
        case Some(p) if p.label().nonEmpty => p.label()
        case _ => parName.toSentenceCase
      }

      val (description, exampleValue) = pluginParam map { pluginParam =>
        val ex = pluginParam.example()
        (pluginParam.value(), if (ex != "") Some(ex) else defaultValue)
      } getOrElse ("No description", defaultValue)

      val advanced = pluginParam exists (_.advanced())
      val visible = pluginParam forall (_.visibleInDialog())
      val dataType = ParameterType.forType(parType)
      val autoCompletion: Option[ParameterAutoCompletion] = pluginParam.flatMap(param => parameterAutoCompletion(parType, param))

      ClassPluginParameter(parName, dataType, label, description, defaultValue, exampleValue, advanced, visible, autoCompletion)
    }
  }

  private def getActions[T](pluginClass: Class[T]): Array[ClassPluginAction] = {
    pluginClass.getMethods.flatMap { method =>
      method.getAnnotations.collect {
        case action: Action =>
          val provideContext = method.getParameters match {
            case Array() =>
              false
            case Array(param) if classOf[PluginContext].isAssignableFrom(param.getType) =>
              true
            case _ =>
              throw new InvalidPluginException(s"Action method ${method.getName} in class ${pluginClass.getName} has an invalid signature. " +
                s"Only methods with no parameters or a single PluginContext parameter are allowed.")
          }
          ClassPluginAction(method, provideContext, action.label(), action.description(), loadIcon(pluginClass, action.iconFile()))
      }
    }
  }

  private def getPluginTypes(pluginClass: Class[_]): Seq[PluginTypeDescription] = {
    // Find the PluginType annotation(s)
    val pluginTypes =
      for {
        superType <- getSuperTypes(pluginClass).toSeq
        typeAnnotation <- PluginTypeDescription.forClassOpt(superType)
      } yield {
        typeAnnotation
      }

    if(pluginTypes.nonEmpty) {
      pluginTypes
    } else {
      throw new IllegalArgumentException(s"Class $pluginClass does not inherit from a class that is a plugin type.")
    }
  }

  private def getSuperTypes(pluginClass: Class[_]): Set[Class[_]] = {
    val superTypes = pluginClass.getInterfaces ++ Option(pluginClass.getSuperclass)
    val nonStdTypes = superTypes.filterNot(c => c.getName.startsWith("java") || c.getName.startsWith("scala"))
    nonStdTypes.toSet ++ nonStdTypes.flatMap(getSuperTypes)
  }

  private def parameterAutoCompletion[T](dataType: Type,
                                         pluginParam: Param): Option[ParameterAutoCompletion] = {
    dataType match {
      case _ if pluginParam.autoCompletionProvider() != classOf[NopPluginParameterAutoCompletionProvider] =>
        Some(explicitParameterAutoCompletionProvider(pluginParam))
      case enumClass: Class[_] if enumClass.isEnum =>
        Some(enumParameterAutoCompletion(enumClass))
      case optionEnumClass: ParameterizedType if
        optionEnumClass.getRawType == classOf[Option[_]] &&
          optionEnumClass.getActualTypeArguments.length == 1 &&
          optionEnumClass.getActualTypeArguments.head.isInstanceOf[Class[_]] &&
          optionEnumClass.getActualTypeArguments.head.asInstanceOf[Class[_]].isEnum =>
        val enumClass = optionEnumClass.getActualTypeArguments.head.asInstanceOf[Class[_]]
        val autoCompletion = enumParameterAutoCompletion(enumClass)
        Some(autoCompletion)
      case _ =>
          None
    }
  }

  private def enumParameterAutoCompletion[T](enumClass: Class[_]): ParameterAutoCompletion = {
    val withLabel = classOf[EnumerationParameterType].isAssignableFrom(enumClass)
    ParameterAutoCompletion(
      autoCompletionProvider = EnumPluginParameterAutoCompletionProvider(enumClass),
      allowOnlyAutoCompletedValues = true,
      autoCompleteValueWithLabels = withLabel
    )
  }

  private def explicitParameterAutoCompletionProvider(pluginParam: Param): ParameterAutoCompletion = {
    assert(pluginParam.autoCompletionProvider() != classOf[NopPluginParameterAutoCompletionProvider])
    val autoCompletionProvider = PluginParameterAutoCompletionProvider.get(pluginParam.autoCompletionProvider())
    val allowOnlyAutoCompletedValues = pluginParam.allowOnlyAutoCompletedValues()
    val autoCompleteValueWithLabels = pluginParam.autoCompleteValueWithLabels()
    val autoCompletionDependsOnParameters = pluginParam.autoCompletionDependsOnParameters()
    ParameterAutoCompletion(
      autoCompletionProvider = autoCompletionProvider,
      allowOnlyAutoCompletedValues = allowOnlyAutoCompletedValues,
      autoCompleteValueWithLabels = autoCompleteValueWithLabels,
      autoCompletionDependsOnParameters = autoCompletionDependsOnParameters
    )
  }

  case class EnumPluginParameterAutoCompletionProvider(enumClass: Class[_]) extends PluginParameterAutoCompletionProvider {
    assert(enumClass.isEnum, "Trying to create enum plugin parameter auto completion provider with non-enum class: " + enumClass.getCanonicalName)
    lazy val enumValues: Seq[AutoCompletionResult] = {
      val method = enumClass.getDeclaredMethod("values")
      val enumArray = method.invoke(null).asInstanceOf[Array[Enum[_]]]
      enumArray.map {
        case enumerationParameter: EnumerationParameterType => AutoCompletionResult(enumerationParameter.id, Some(enumerationParameter.displayName))
        case enumValue: Enum[_] => AutoCompletionResult(enumValue.name(), None)
      }
    }
    override def autoComplete(searchQuery: String,
                              dependOnParameterValues: Seq[ParamValue],
                              workspace: WorkspaceReadTrait)
                             (implicit context: PluginContext): Iterable[AutoCompletionResult] = {
      filterResults(searchQuery, enumValues)
    }

    override def valueToLabel(value: String,
                              dependOnParameterValues: Seq[ParamValue],
                              workspace: WorkspaceReadTrait)
                             (implicit context: PluginContext): Option[String] = {
      enumValues.find(_.value == value).flatMap(_.label)
    }
  }

  private def getDefaultValues[T](pluginClass: Class[T], count: Int): Array[Option[AnyRef]] = {
    try {
      val clazz = Class.forName(pluginClass.getName + "$", true, pluginClass.getClassLoader)
      val module = clazz.getField("MODULE$").get(null)
      val methods = clazz.getMethods.map(method => (method.getName, method)).toMap

      for (i <- Array.range(1, count + 1)) yield {
        methods.get("$lessinit$greater$default$" + i).map(_.invoke(module))
      }
    }
    catch {
      case _: ClassNotFoundException => Array.fill(count)(None)
    }
  }
}

case class ClassPluginAction(method: Method, provideContext: Boolean, label: String, description: String, icon: Option[String]) extends PluginAction {

  override def call(plugin: AnyRef)(implicit context: PluginContext): String = {
    if(provideContext) {
      method.invoke(plugin, context).toString
    } else {
      method.invoke(plugin).toString
    }
  }
}
