package de.fuberlin.wiwiss.silk.datasource

import de.fuberlin.wiwiss.silk.instance.{InstanceSpecification, Instance}
import xml.Node
import de.fuberlin.wiwiss.silk.util.ValidatingXMLReader

/**
 * A source of instances.
 */
case class Source(id : String, dataSource : DataSource)
{
  /**
   * Retrieves instances from this source which satisfy a specific instance specification.
   *
   * @param instanceSpec The instance specification
   * @param instances The URIs of the instances to be retrieved. If empty, all instances will be retrieved.
   *
   * @return A Traversable over the instances. The evaluation of the Traversable may be non-strict.
   */
  def retrieve(instanceSpec : InstanceSpecification, instances : Seq[String] = Seq.empty) = dataSource.retrieve(instanceSpec, instances)

  def toXML : Node = dataSource match
  {
    case DataSource(dataSourceType, params) =>
    {
      <DataSource id={id} type={dataSourceType}>
        { params.map{case (name, value) => <Param name={name} value={value} />} }
      </DataSource>
    }
  }
}

object Source
{
  private val schemaLocation = "de/fuberlin/wiwiss/silk/linkspec/LinkSpecificationLanguage.xsd"

  def load =
  {
    new ValidatingXMLReader(node => fromXML(node), schemaLocation)
  }

  def fromXML(node : Node) : Source =
  {
    new Source(node \ "@id" text, DataSource(node \ "@type" text, readParams(node)))
  }

  private def readParams(element : Node) : Map[String, String] =
  {
    element \ "Param" map(p => (p \ "@name" text, p \ "@value" text)) toMap
  }
}