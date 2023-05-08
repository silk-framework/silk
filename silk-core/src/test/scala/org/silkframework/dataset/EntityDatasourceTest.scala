package org.silkframework.dataset

import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.config.{PlainTask, Prefixes}
import org.silkframework.entity.paths.{TypedPath, UntypedPath}
import org.silkframework.entity.{Entity, EntitySchema}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.util.{CloseableIterator, Uri}

class EntityDatasourceTest extends FlatSpec with MustMatchers {
  behavior of "Entity Data Source"

  implicit val userContext: UserContext = UserContext.Empty
  implicit val prefixes: Prefixes = Prefixes.empty

  val alibiTask = PlainTask("alibi", DatasetSpec(EmptyDataset))
  val typeUri = Uri("http://entity.com/type1")
  val paths: IndexedSeq[TypedPath] = nrToPaths(IndexedSeq(1, 2, 3, 4))
  val entityUri = "http://entity.com/1"
  val entitySchema = EntitySchema(typeUri, typedPaths = paths)
  val entities = Seq(
    Entity(entityUri,
      values = IndexedSeq(Seq("1"), Seq("2"), Seq("3"), Seq("4")),
      entitySchema
    )
  )
  val entityDatasource = EntityDatasource(alibiTask, CloseableIterator(entities), entitySchema)

  it should "turn its entities into the request schema" in {
    val requestSchema = EntitySchema(typeUri, nrToPaths(IndexedSeq(4, 2)))
    val requestEntities = entityDatasource.retrieve(requestSchema).entities.toSeq
    requestEntities.size mustBe 1
    val entity = requestEntities.head
    entity.uri.toString mustBe entityUri
    entity.values mustBe IndexedSeq(Seq("4"), Seq("2"))
  }

  private def nrToPaths(seq: IndexedSeq[Int]): IndexedSeq[TypedPath] = {
    seq.map(nr => UntypedPath(s"http://path$nr").asStringTypedPath)
  }
}
