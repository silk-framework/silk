package org.silkframework.rule.execution.rdb

import org.silkframework.dataset.DataSource
import org.silkframework.entity.{Entity, EntitySchema, TypedPath}
import org.silkframework.rule._
import org.silkframework.runtime.activity.{Activity, ActivityContext, UserContext}
import org.silkframework.util.{DPair, Identifier}

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

/**
  * Loads the entities and the entity index into the relational database.
  *
  * @param linkSpec             The link
  * @param dataSource           The input data source
  * @param sourceOrTarget       Is this the source input of the link spec, true for source, false for target.
  * @param runtimeLinkingConfig Linking execution config.
  */
class RDBEntityIndexLoader(linkSpec: LinkSpec,
                           dataSource: DataSource,
                           sourceOrTarget: Boolean,
                           runtimeLinkingConfig: RuntimeLinkingConfig) extends Activity[EntityTables] {

  def entitySchema: EntitySchema = if(sourceOrTarget) linkSpec.entityDescriptions.source else linkSpec.entityDescriptions.target
  def linkageRule: LinkageRule = linkSpec.rule
  val booleanLinkageRule: BooleanLinkageRule = BooleanLinkageRule(linkageRule).getOrElse(
    throw new IllegalArgumentException("Linkage rule is no boolean linkage rule."))

  override def run(context: ActivityContext[EntityTables])
                  (implicit userContext: UserContext): Unit = {
    profileEntities(entitySchema)

  }

  private def profileEntities(entitySchema: EntitySchema)
                             (implicit userContext: UserContext): RdbIndexProfile = {
    val entities = retrieveEntities()
    val pathProfiles = entitySchema.typedPaths.map(tp => (tp, EntityPathProfile())).toMap
    val indexProfiles = booleanLinkageRule.comparisons.map(c => (c.id, IndexProfile())).toMap
    for(entity <- entities) {
      val linkageRuleIndex = LinkageRuleIndex.apply(entity, booleanLinkageRule, sourceOrTarget)
      linkageRuleIndex.comparisons.foreach { c =>
        val indexProfile = indexProfiles.getOrElse(
          c.id,
          throw new RuntimeException("Index profile for comparison ID " + c.id + " not found!"))
        indexProfile.update(c.indexValues.indexValues)
      }
      entitySchema.typedPaths.foreach { typedPath =>
        val pathProfile = pathProfiles.getOrElse(
          typedPath,
          throw new RuntimeException("Path profile for typed path " + typedPath.toString + " not found!")
        )
        pathProfile.update(entity.valueOf(typedPath))
      }
    }
    RdbIndexProfile(pathProfiles, indexProfiles)
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

/**
  * The profiling results for the entities and their index values.
  * @param pathProfiles A map from the data source entities' typed paths.
  * @param indexProfile A map from the comparison ID to the corresponding index value profile.
  */
case class RdbIndexProfile(pathProfiles: Map[TypedPath, EntityPathProfile],
                           indexProfiles: Map[Identifier, IndexProfile])

trait CardinalityProfile {
  private var minCardinality = Int.MaxValue
  private var maxCardinality = 0

  def updateCardinality(cardinality: Int): Unit = {
    if(cardinality < minCardinality) {
      minCardinality = cardinality
    }
    if(cardinality > maxCardinality) {
      maxCardinality = cardinality
    }
  }

  /** Min/max cardinality */
  def cardinality: (Int, Int) = {
    (minCardinality, maxCardinality)
  }
}

case class EntityPathProfile() extends CardinalityProfile {
  private var minStringLength = Int.MaxValue
  private var maxStringLength = 0

  def update(values: Seq[String]): Unit = {
    updateCardinality(values.size)
    values foreach { value =>
      if(value.length < minStringLength) {
        minStringLength = value.length
      }
      if(value.length > maxStringLength) {
        maxStringLength = value.length
      }
    }
  }

  def stringLengthRange: (Int, Int) = (minStringLength, maxStringLength)
}

/** Profiling of the index values for a specific linkage rule comparison input.
  * We need the cardinality to decide if we add the values to the master index table or create a separate table. */
case class IndexProfile() extends CardinalityProfile {
  def update(indexValues: Set[Int]): Unit = {
    val cardinality = indexValues.size
    updateCardinality(cardinality)
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