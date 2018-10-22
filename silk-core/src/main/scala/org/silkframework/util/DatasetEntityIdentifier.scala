package org.silkframework.util

import java.net.URISyntaxException

import org.silkframework.dataset.DataSource

import scala.util.{Failure, Success, Try}

/**
  * A special URI class used for internal entity identification based on the dataset it belongs (these are URNs)
  * @param dataset - the task id of the dataset the entity belongs to
  * @param entityId - a positive Long identifying the Entity uniquely
  * @param urnPrefix - a URN prefix (ending in an colon!)
  */
class DatasetEntityIdentifier(
  val dataset: Identifier,
  val entityId: Long,
  urnPrefix: String = DataSource.URN_NID_PREFIX
) extends Uri(DatasetEntityIdentifier.validateAndCreate(dataset, entityId, urnPrefix))

object DatasetEntityIdentifier{

  def validateAndCreate(
   dataset: Identifier,
   entityId: Long,
   urnPrefix: String
 ): String = {
    assert(urnPrefix.trim.endsWith(":"), "A default id prefix (option 'project.resourceUriPrefix') has to end with an colon, since it is used to create URNs.")
    assert(entityId >= 0, "Entity internal identifiers only except positive numbers.")
    urnPrefix + dataset.toString + "#" + entityId
  }

  def apply(
     dataset: Identifier,
     entityId: Long,
     urnPrefix: String = DataSource.URN_NID_PREFIX
   ): DatasetEntityIdentifier = new DatasetEntityIdentifier(dataset, entityId, urnPrefix)

  def apply(id: String): DatasetEntityIdentifier = {
    Try{
      val hashInd = id.lastIndexOf('#')
      val lastColon = id.lastIndexOf(':', hashInd)
      val prefix = id.substring(0, lastColon+1)
      val datasetId = id.substring(lastColon+1, hashInd)
      val entityId = id.substring(hashInd+1).trim.toLong
      new DatasetEntityIdentifier(datasetId, entityId, prefix)
    } match{
      case Success(i) => i
      case Failure(_) => throw new URISyntaxException(id, "Internal Entity identifier have to be URNs of the following syntax: urn:prefix:exchangeable:dataset_identifier#internal_entity_long_id")
    }
  }
}
