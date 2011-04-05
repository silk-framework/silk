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

  def getInterlinks = new RuleTransformer(new RemoveNodeByLabel("DataSources")).transform(doc).head

  def getPrefixes = doc \ "Prefixes" \ "Prefix" 

  // append prefixes to the project - see http://www.assembla.com/spaces/silk/tickets/26
  def appendPrefixes (prefixes : NodeSeq) {
      if ((doc \ "Prefixes").size == 0){
        doc = new RuleTransformer(new AddChildTo("Silk", <Prefixes />)).transform(doc).head
      }
      for (pref <- prefixes) doc = new RuleTransformer(new AddChildTo("Prefixes", pref)).transform(doc).head
  }
  
   // The name of this project
  // TODO - set a proper name
  override val name : Identifier = "silk_"

   // Reads the project configuration.
  override def config =
  {
    val prefixes = (doc \ "Prefixes" headOption).map(Prefixes.fromXML).getOrElse(Prefixes.empty)
    ProjectConfig(prefixes)
  }

   // Writes the updated project configuration.
  override def config_=(config : ProjectConfig)
  {
    doc = new RuleTransformer(new RemoveNodeByLabel("Prefixes")).transform(doc).head
    appendPrefixes(config.prefixes.toXML \ "Prefix")
  }

   // The source module which encapsulates all data sources.
  override val sourceModule = new XMLSourceModule()

   // The linking module which encapsulates all linking tasks.
  override val linkingModule = new XMLLinkingModule()


  
  /** ----------------------------------------------------------
   *   The source module which encapsulates all data sources.
   *  ---------------------------------------------------------- */
  class XMLSourceModule() extends SourceModule {

    def config = SourceConfig()

    def config_=(c : SourceConfig) {}

    def tasks = synchronized {
      for (ds <- doc \\ "DataSource")   yield
      {
        val source = Source.fromXML(ds)
        SourceTask(source)
      }
    }

    def update(task : SourceTask) = synchronized {
        // if any datasource is defined yet
      if ((doc \ "DataSources").size == 0)  {
          doc = new RuleTransformer(new AddChildTo("Silk", <DataSources />)).transform(doc).head
      }
       // if this task exists
      else if ((doc \ "DataSources" \ "DataSource").filter(n => (n \ "@id").text.equals(task.name.toString)).size > 0){
         // TODO  update interlink (better)
         remove(task.name)
      }
      doc = new RuleTransformer(new AddChildTo("DataSources", task.source.toXML)).transform(doc).head
    }

    def remove(taskId : Identifier) = synchronized {
       // Remove datasource with id = task.name
       doc = new RuleTransformer(new RemoveNodeById("DataSource",taskId)).transform(doc).head
    }
  }


  /** ----------------------------------------------------------
   *   The linking module which encapsulates all linking tasks.
   *  ----------------------------------------------------------- */
  class XMLLinkingModule() extends LinkingModule {
    
    def config = LinkingConfig()

    def config_=(c : LinkingConfig) {}

    def tasks = synchronized
    {
      implicit val prefixes = XMLProject.this.config.prefixes

     for(lt <- doc \ "Interlinks" \ "Interlink" ) yield {
        val linkT = LinkSpecification.fromXML(lt)
        val linkingTask = LinkingTask((lt \ "@id").text, linkT, new Alignment(), new Cache())
        linkingTask
      }
    }

    def update(task : LinkingTask) = synchronized
    {
      implicit val prefixes = XMLProject.this.config.prefixes

      // if any interlink is defined yet
      if ((doc \ "Interlinks").size == 0){
         doc = new RuleTransformer(new AddChildTo("Silk", <Interlinks />)).transform(doc).head
      }
      // if this task exists
      else if ((doc \ "Interlinks" \ "Interlink").filter(n => (n \ "@id").text.equals(task.name.toString)).size > 0) {
         remove(task.name)
      }

      doc = new RuleTransformer(new AddChildTo("Interlinks", task.linkSpec.toXML)).transform(doc).head
    }

    def remove(taskId : Identifier) = synchronized  {
       logger.info("Removing LinkingTask: "+ taskId)
       // Remove interlink with id = task.name
       doc = new RuleTransformer(new RemoveNodeById("Interlink",taskId.toString)).transform(doc).head
       //if ((doc \\ "Interlink").size == 0){
       //   doc = new RuleTransformer(new RemoveNodeByLabel("Interlinks")).transform(doc).head
       //}
    }
  }

  // Utils

  // Change a specific node to add the new child
  class AddChildTo(label: String, newChild: Node) extends RewriteRule {
    override def transform(n: Node) = n match {
      case e @ Elem(_, `label`, _, _, _*) => new Elem (e.prefix, e.label, e.attributes, e.scope, transform(e.child) ++ newChild :_*)
      case n => n
    }
  }
  
  // Remove a specific node by id (and label)
  class RemoveNodeById(label: String, id: String) extends RewriteRule {
    override def transform(n: Node) : NodeSeq = n match {
      case e @ Elem(_, `label`, _, _, _*) =>  if ((e \ "@id").text.equals(id)) NodeSeq.Empty else e
      case n => n
    }
  }

  // Remove a (unique) node by label
  class RemoveNodeByLabel(label: String) extends RewriteRule {
    override def transform(n: Node) : NodeSeq = n match {
      case e @ Elem(_, `label`, _, _, _*) => NodeSeq.Empty 
      case n => n
    }
  }
}