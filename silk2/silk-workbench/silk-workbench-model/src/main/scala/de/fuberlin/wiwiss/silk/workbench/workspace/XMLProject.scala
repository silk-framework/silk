package de.fuberlin.wiwiss.silk.workbench.workspace

import modules.linking.{Cache, LinkingTask, LinkingConfig, LinkingModule}
import modules.source.{SourceConfig, SourceTask, SourceModule}
import de.fuberlin.wiwiss.silk.datasource.Source
import de.fuberlin.wiwiss.silk.linkspec.LinkSpecification
import de.fuberlin.wiwiss.silk.config.Prefixes
import java.util.logging.Logger
import xml.transform.{RuleTransformer, RewriteRule}
import xml.{NodeSeq, Node, Elem}
import de.fuberlin.wiwiss.silk.util.Identifier
import de.fuberlin.wiwiss.silk.evaluation.Alignment

/**
 * Implementation of a project which maps an XML Silk Link Specification document.
 */
class XMLProject(linkSpec : Node) extends Project
{
  private val logger = Logger.getLogger(classOf[XMLProject].getName)

  private var doc = linkSpec

  def getLinkSpec = doc

  /**
   * The name of this project
   */
  // TODO - set a proper name
  override val name : Identifier = doc \\ "Interlink" \\ "@id" text

  /**
   * Reads the project configuration.
   */
  override def config =
  {
    ProjectConfig()
  }

  /**
   * Writes the updated project configuration.
   */
  override def config_=(config : ProjectConfig)     {}

  /**
   * The source module which encapsulates all data sources.
   */
  override val sourceModule = new XMLSourceModule()
  

  /**
   * The linking module which encapsulates all linking tasks.
   */
  override val linkingModule = new XMLLinkingModule()


  /**
   * The source module which encapsulates all data sources.
   */
  class XMLSourceModule() extends SourceModule
  {

    def config = SourceConfig()

    def config_=(c : SourceConfig) {}

    def tasks = synchronized
    {
      for (ds <- doc \\ "DataSource")   yield
      {
        val source = Source.fromXML(ds)
        SourceTask(source)
      }
    }

    def update(task : SourceTask) = synchronized
    {
      // if this task exists
      if ((doc \\ "DataSource").filter(n => (n \ "@id").text.equals(task.name)).length > 0){
         // TODO  update interlink (better)
         remove(task.name)
      }
      if ((doc \\ "DataSources").size == 0)
            doc = new RuleTransformer(new AddChildrenTo("Silk", <DataSources />)).transform(doc).head
      doc = new RuleTransformer(new AddChildrenTo("DataSources", task.source.toXML)).transform(doc).head
    }

    def remove(taskId : Identifier) = synchronized
    {
       // Remove datasource with id = task.name
       doc = new RuleTransformer(new RemoveNodeById("DataSource",taskId)).transform(doc).head
    }
  }

  /**
   * The linking module which encapsulates all linking tasks.
   */
  class XMLLinkingModule() extends LinkingModule
  {
    def config = LinkingConfig()

    def config_=(c : LinkingConfig) {}

    def tasks = synchronized
    {
     val prefixes = Prefixes.fromXML((doc \\ "Prefixes") (0))

     for(lt <- doc \\ "Interlink" ) yield
      {
        val linkT = LinkSpecification.fromXML(lt,(prefixes))
        val linkingTask = LinkingTask((lt \ "@id").text, prefixes, linkT, new Alignment(), new Cache())
        linkingTask.loadCache(XMLProject.this)
        linkingTask
      }
    }

    def update(task : LinkingTask) = synchronized
    {
      // if this task exists
      if ((doc \\ "Interlink").filter(n => (n \ "@id").text.equals(task.name)).length > 0){
         // TODO  update interlink (better)
         remove(task.name)
      }
      doc = new RuleTransformer(new AddChildrenTo("Interlinks", task.linkSpec.toXML)).transform(doc).head
      task.loadCache(XMLProject.this)
    }

    def remove(taskId : Identifier) = synchronized
    {
       logger.info("Removing LinkingTask: "+ taskId)
       // Remove interlink with id = task.name
       doc = new RuleTransformer(new RemoveNodeById("Interlink",taskId)).transform(doc).head
    }
  }

  // Utils

  // Change a specific node to add the new child
  class AddChildrenTo(label: String, newChild: Node) extends RewriteRule {
    override def transform(n: Node) = n match {
      case e @ Elem(_, `label`, _, _, _*) => new Elem (e.prefix, e.label, e.attributes, e.scope, transform(e.child) ++ newChild :_*) 
      case n => n
    }
  }

  // Remove a specific node
  class RemoveNodeById(label: String, id: String) extends RewriteRule {
    override def transform(n: Node) : NodeSeq = n match {
      case e @ Elem(_, `label`, _, _, _*) =>  if ((e \ "@id").text == id) NodeSeq.Empty else e
      case n => n
    }
  }
  
}