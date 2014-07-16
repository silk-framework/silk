package plugins

/**
 * Holds all available Workbench plugins.
 */
object WorkbenchPlugins {
  val all: Seq[WorkbenchPlugin] = Seq(new DataPlugin(), new TransformPlugin(), new LinkingPlugin())
}
