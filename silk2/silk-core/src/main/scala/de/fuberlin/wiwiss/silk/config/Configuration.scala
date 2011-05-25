package de.fuberlin.wiwiss.silk.config

import de.fuberlin.wiwiss.silk.linkspec.LinkSpecification
import de.fuberlin.wiwiss.silk.output.Output
import de.fuberlin.wiwiss.silk.datasource.Source
import xml.Node
import de.fuberlin.wiwiss.silk.util.ValidatingXMLReader

/**
 * A Silk linking configuration.
 * Specifies how multiple sources are interlinked by defining a link specification for each type of instance to be interlinked.
 *
 * @param prefixes The prefixes which are used throughout the configuration to shorten URIs
 * @param sources The sources which should be interlinked
 * @param linkSpecs The Silk link specifications
 * @param outputs The global output
 */
case class Configuration(prefixes : Prefixes,
                         sources : Traversable[Source],
                         blocking : Option[Blocking],
                         linkSpecs : Traversable[LinkSpecification],
                         outputs : Traversable[Output] = Traversable.empty)
{
  private val sourceMap = sources.map(s => (s.id, s)).toMap
  private val linkSpecMap = linkSpecs.map(s => (s.id, s)).toMap

  /**
   * Selects a datasource by id.
   */
  def source(id : String) = sourceMap(id)

  /**
   * Selects a link specification by id.
   */
  def linkSpec(id : String) = linkSpecMap(id)

  /**
   * Merges this configuration with another configuration.
   */
  def merge(config : Configuration) =
  {
    Configuration(
      prefixes = prefixes ++ config.prefixes,
      sources = sources ++ config.sources,
      blocking = blocking,
      linkSpecs = linkSpecs ++ config.linkSpecs,
      outputs = outputs ++ config.outputs
    )
  }

  def toXML : Node =
  {
    <Silk>
      { prefixes.toXML }
      <DataSources>
        { sources.map(_.toXML) }
      </DataSources>
      <Interlinks>
        { linkSpecs.map(_.toXML(prefixes)) }
      </Interlinks>
    </Silk>
  }
}

object Configuration
{
  private val schemaLocation = "de/fuberlin/wiwiss/silk/linkspec/LinkSpecificationLanguage.xsd"

  def empty = Configuration(Prefixes.empty, Nil, Some(Blocking()), Nil, Nil)

  def load =
  {
    new ValidatingXMLReader(fromXML, schemaLocation)
  }

  def fromXML(node : Node) =
  {
    implicit val prefixes = Prefixes.fromXML(node \ "Prefixes" head)
    val sources = (node \ "DataSources" \ "DataSource").map(Source.fromXML)
    val blocking = (node \ "Blocking").headOption match
    {
      case Some(blockingNode) => Blocking.fromXML(blockingNode)
      case None => Some(Blocking())
    }
    val linkSpecifications = (node \ "Interlinks" \ "Interlink").map(p => LinkSpecification.fromXML(p))
    val outputs = (node \ "Outputs" \ "Output").map(Output.fromXML)

    Configuration(prefixes, sources, blocking, linkSpecifications, outputs)
  }
}
