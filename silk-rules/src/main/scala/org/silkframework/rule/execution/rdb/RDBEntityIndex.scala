package org.silkframework.rule.execution.rdb

import org.silkframework.dataset.DataSource
import org.silkframework.entity.{Entity, EntitySchema, TypedPath}
import org.silkframework.rule.{LinkSpec, RuntimeLinkingConfig}
import org.silkframework.runtime.activity.{Activity, ActivityContext, UserContext}
import org.silkframework.util.DPair

/**
  * An entity index based on a relational database that is used to generate linking candidates based on a link specification.
  */
class RDBEntityIndex(linkSpec: LinkSpec,
                     dataSources: DPair[DataSource],
                     runtimeLinkingConfig: RuntimeLinkingConfig) extends Activity[Unit] {

  override def run(context: ActivityContext[Unit])
                  (implicit userContext: UserContext): Unit = {

  }
}


class RDBEntityIndexLoader(linkSpec: LinkSpec,
                           entitySchema: EntitySchema,
                           dataSource: DataSource,
                           runtimeLinkingConfig: RuntimeLinkingConfig) extends Activity[EntityTables] {
  override def run(context: ActivityContext[EntityTables])
                  (implicit userContext: UserContext): Unit = {
    profileEntities(entitySchema)

  }

  private def profileEntities(entitySchema: EntitySchema)
                             (implicit userContext: UserContext): Unit = {
    val entities = retrieveEntities()

  }

  private def retrieveEntities()
                              (implicit userContext: UserContext): Traversable[Entity] = {
    runtimeLinkingConfig.sampleSizeOpt match {
      case Some(sampleSize) =>
        dataSource.sampleEntities(entitySchema, sampleSize, None)
      case None =>
        dataSource.retrieve(entitySchema)
    }
  }
}

class EntityPathProfile() {
  private var minStringLength = Int.MaxValue
  private var maxStringLength = 0
  private var minCardinality = Int.MaxValue
  private var maxCardinality = 0

  def update(vals: Seq[String]): Unit = {

  }
}

case class EntityTables(entityTable: EntityRdbTable,
                        mainIndexTable: IndexRdbTable,
                        multiEntryIndexTables: Map[TypedPath, SinglePathIndexTable])

case class EntityRdbTable(uriColumn: String, tableName: String, columns: Map[TypedPath, RdbValueColumn])

case class IndexRdbTable(tableName: String, columns: Map[TypedPath, RdfIndexColumn])

case class SinglePathIndexTable(tableName: String)

case class RdbValueColumn(rdbColumnName: String,
                          nullable: Boolean,
                          arrayType: Boolean,
                          minValueLength: Int,
                          maxValueLength: Int)

case class RdfIndexColumn(rdfColumnName: String,
                          nullable: Boolean)

case class RdbRuntimeConfig(jdbcConnectionString: String, user: String, password: String)