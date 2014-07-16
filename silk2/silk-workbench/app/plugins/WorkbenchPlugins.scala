package plugins

/**
 * Holds all available Workbench plugins.
 */
object WorkbenchPlugins {
  val all: Seq[WorkbenchPlugin] = Seq(new LinkingPlugin(), new TransformPlugin())
}



