package de.fuberlin.wiwiss.silk.linkspec

import xml.Node
import de.fuberlin.wiwiss.silk.util.Identifier

/**
 * Defines a dataset.
 *
 * @param sourceId The id of the source
 * @param variable Each data item will be bound to this variable.
 * @param restriction Restricts this dataset using SPARQL clauses.
 */
case class DatasetSpecification(sourceId : Identifier, variable : String, restriction : String)
{

  /**
   * Serializes this Dataset Specification as XML.
   *
   * @param If true, this dataset will be serialized as a source dataset. If false it will be serialize as target dataset.
   */
  def toXML(asSource : Boolean) =
  {
    if(asSource)
    {
      <SourceDataset dataSource={sourceId} var={variable}>
        <RestrictTo>{restriction}</RestrictTo>
      </SourceDataset>
    }
    else
    {
      <TargetDataset dataSource={sourceId} var={variable}>
        <RestrictTo>{restriction}</RestrictTo>
      </TargetDataset>
    }
  }
}

object DatasetSpecification
{
  /**
   * Creates a DatasetSpecification from XML.
   */
  def fromXML(node : Node) : DatasetSpecification =
  {
    new DatasetSpecification(
      node \ "@dataSource" text,
      node \ "@var" text,
      (node \ "RestrictTo").text.trim
    )
  }
}
