package org.silkframework.dataset

import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.entity.{Entity, EntitySchema, Path, TypedPath}
import org.silkframework.util.Uri

/**
  *
  */
class EntityDatasourceTest extends FlatSpec with MustMatchers {
  behavior of "Entity Data Source"

  it should "turn its entities into the request schema" in {
    val typeUri = Uri("http://entity.com/type1")
    val paths = nrToPaths(IndexedSeq(1, 2, 3, 4))
    val entityUri = "http://entity.com/1"
    val entitySchema = EntitySchema(typeUri, typedPaths = paths)
    val entities = Seq(
      Entity(entityUri,
        values = IndexedSeq(Seq("1"), Seq("2"), Seq("3"), Seq("4")),
        entitySchema
      )
    )
    val requestSchema = EntitySchema(typeUri, nrToPaths(IndexedSeq(4, 2)))
    val entityDatasource = EntityDatasource(entities, entitySchema)
    val requestEntities = entityDatasource.retrieve(requestSchema)
    requestEntities.size mustBe 1
    val entity = requestEntities.head
    entity.uri.toString mustBe entityUri
    entity.values mustBe IndexedSeq(Seq("4"), Seq("2"))
  }

  private def nrToPaths(seq: IndexedSeq[Int]): IndexedSeq[TypedPath] = {
    seq.map(nr => Path(s"http://path$nr").asStringTypedPath)
  }
}
