The framework provides a plugin system for extending it with additional behaviour.

How to write a plugin
=====================

A new plugin is added by implementing a plugin interface and adding the `Plugin` annotation:

    @Plugin(
      id = "pluginId",
      label = "Human Readable Name",
      description = "Short description."
    )
    case class MyPlugin(param1: String, param2: Double) extends AnyPlugin {
      (method implementations)
    }

**Base Interface**

Instead of implementing from `AnyPlugin`, an actual plugin would implement a specific plugin interface, such as `DistanceMeasure`, if a new distance measure should be added. Common plugin interfaces will be described in detail in the next sections.

**Parameters**

A Plugin may define a number of parameters, such as `param1` and `param2` in the above example. At the moment, the following parameter types are supported:

-   `String`: A character string.
-   `Char`: A single character.
-   `Int`: A whole number.
-   `Double`: A floating point number.
-   `Boolean`: A boolean typed value.
-   `Resource`: A file resource.

**Plugin Annotation**

The plugin annotation has the following parameters:

-   `id`: An id that uniquely identifies this plugin. May only contain the following characters: (a - z, A - Z, 0 - 9, \_, -).
-   `label`: A human readable label to be shown in graphical user interfaces.
-   `description`: A human readable description of this plugin.
-   `categories` (optional): An array of categories for this plugin. Each category is a string value.

The plugin annotation will be left out from all following examples as it takes the same parameters for all types of plugins.

**Adding the Plugin**
In order for the plugin to be discovered on startup, a jar containing the plugins must be placed in the `{user_home}/.silk/plugins/` directory. Silk provides a number of different plugin interfaces that can be implemented in order to add new behavour. In the following, we describe the most common ones.

Adding Transformers
===================

Most transformations can be expressed with the `SimpleTransformer` interface. `SimpleTransformer` only requires the implementation of the `evaluate` method.
Example:

    case class LowerCaseTransformer() extends SimpleTransformer {
      override def evaluate(value: String) = {
        value.toLowerCase
      }
    }

Adding Distance Measures
========================

Most distance measures can be expressed with the `SimpleDistanceMeasure` interface. Example:

    case class MyMetric() extends SimpleDistanceMeasure {

      override def evaluate(str1: String, str2: String, limit: Double) = ...

      override def indexValue(str: String, threshold: Double) = ...
    }

The evaluate method takes three arguments:

-   `str1` and `str2`: The two strings that are to be compared.
-   `limit`: If the expected distance between both strings exceeds this limit, a distance measure may return `Double.PositiveInfinity` instead of the actual distance in order to save computation time.

The return value is a positive number