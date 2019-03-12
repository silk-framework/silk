package org.silkframework.rule.execution.rdb

import org.silkframework.dataset.DataSource
import org.silkframework.entity.{Entity, EntitySchema, TypedPath}
import org.silkframework.rule._
import org.silkframework.runtime.activity.{Activity, ActivityContext, UserContext}
import org.silkframework.util.{DPair, Identifier}
import java.sql._
import java.util.logging.Logger

import org.silkframework.config.DefaultConfig
import org.silkframework.rule.execution.rdb.RDBEntityIndex.createConnection

/**
  * An entity index based on a relational database that is used to generate linking candidates based on a link specification.
  */
class RDBEntityIndex(linkSpec: LinkSpec,
                     dataSources: DPair[DataSource],
                     runtimeLinkingConfig: RuntimeLinkingConfig) extends Activity[Unit] {
  private val cfg = DefaultConfig.instance()

  override def run(context: ActivityContext[Unit])
                  (implicit userContext: UserContext): Unit = {
    init()
    val uniquePrefix = RDBEntityIndex.uniquePrefix
    val sourceLoader = new RDBEntityIndexLoader(linkSpec, dataSources.source, sourceOrTarget = true, runtimeLinkingConfig, uniquePrefix)
    val targetLoader = new RDBEntityIndexLoader(linkSpec, dataSources.target, sourceOrTarget = false, runtimeLinkingConfig, uniquePrefix)
    val sourceLoaderActivity = context.child(activity = sourceLoader, progressContribution = 0.25)
    val targetLoaderActivity = context.child(activity = targetLoader, progressContribution = 0.25)
    sourceLoaderActivity.start()
    targetLoaderActivity.start()
    sourceLoaderActivity.waitUntilFinished()
    targetLoaderActivity.waitUntilFinished()
  }

  private def init(): Unit = {
    val missingConfigs = Seq(
      RDBEntityIndex.jdbcConnectionStringConfigKey,
      RDBEntityIndex.linkingExecutionRdbUserKey,
      RDBEntityIndex.linkingExecutionRdbPasswordKey,
      RDBEntityIndex.linkingExecutionRdbJdbcDriverClass
    ).filter(!cfg.hasPath(_))
    if (missingConfigs.nonEmpty) {
      throw new RuntimeException("Configuration missing for RDB based linking execution. Please configure: " + missingConfigs.mkString(", "))
    }
    val driverClass = cfg.getString(RDBEntityIndex.linkingExecutionRdbJdbcDriverClass)
    try {
      Class.forName(driverClass)
    } catch {
      case ex: ClassNotFoundException =>
        throw new RuntimeException("Configured JDBC driver class could not be found: " + driverClass, ex)
    }
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
                           runtimeLinkingConfig: RuntimeLinkingConfig,
                           uniquePrefix: String) extends Activity[EntityTables] {
  private val tablePrefix = uniquePrefix + (if(sourceOrTarget) "source_" else "target_")
  private val mainIndexTableName = tablePrefix + "mainIndexTable"
  private val entityIdColumn = "entityId"
  private val log: Logger = Logger.getLogger(this.getClass.getName)

  def entitySchema: EntitySchema = if(sourceOrTarget) linkSpec.entityDescriptions.source else linkSpec.entityDescriptions.target
  def linkageRule: LinkageRule = linkSpec.rule
  val booleanLinkageRule: BooleanLinkageRule = BooleanLinkageRule(linkageRule).getOrElse(
    throw new IllegalArgumentException("Linkage rule is no boolean linkage rule."))

  override def run(context: ActivityContext[EntityTables])
                  (implicit userContext: UserContext): Unit = {
    val indexProfile = profileEntities(entitySchema)
    val connection = RDBEntityIndex.createConnection()
    var (forMainIndexTable, separateIndexTable) = indexProfile.indexProfiles.partition(_._2.cardinality._2 <= 1)
    // Create main index table
    try {
      createMainIndexTable(connection, forMainIndexTable)
    } catch {
      case ex: SQLException =>
        throw new RuntimeException("Tables for entity linking could not be created because of SQL error. Details: " + ex.getMessage, ex)
      case ex: SQLTimeoutException =>
        throw new RuntimeException("Tables for entity linking could not be created because of SQL timeout. Details: " + ex.getMessage, ex)
    } finally {
      connection.close()
    }
  }

  private def createMainIndexTable(connection: Connection, forMainIndexTable: Map[Identifier, IndexProfile]): Unit = {
    if (forMainIndexTable.nonEmpty) {
      val createTable = new StringBuilder(s"""CREATE TABLE $mainIndexTableName(\n""")
      createTable.append(s" $entityIdColumn integer NOT NULL,\n")
      for ((comparisonId, indexProfile) <- forMainIndexTable) {
        createTable.append(s"  $comparisonId integer ${if (indexProfile.cardinality._1 == 1) "NOT NULL" else ""},\n")
      }
      createTable.append(");")
      connection.createStatement().executeUpdate(createTable.toString())
      registerTable(mainIndexTableName, connection)
      log.fine("Created RDB main entity index table: " + createTable.toString())
    }
  }

  private def registerTable(tableName: String, connection: Connection): Unit = {
    connection.createStatement().execute(
      s"""
        |INSERT INTO ${RDBEntityIndex.linkingTableRegistry} VALUES ()
      """.stripMargin)
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
  * @param pathProfiles  A map from the data source entities' typed paths.
  * @param indexProfiles A map from the comparison ID to the corresponding index value profile.
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

object RDBEntityIndex {
  private val log: Logger = Logger.getLogger(this.getClass.getName)
  private val cfg = DefaultConfig.instance()
  final val linkingTableRegistry = "linkingTableRegistry" // Table that stores all created tables

  private var initialized = false

  var count = 1L

  /** Clean up old tables, create missing tables. */
  def init(): Unit = synchronized {
    if(!initialized) {
      val conn = createConnection()
      try {
        val statement = conn.createStatement()
        val createTableRegister = // TODO: IF NOT EXISTS needs Postgres 9.1, is this Ok? This exists for ages in MySQL.
          s"""CREATE TABLE IF NOT EXISTS $linkingTableRegistry(
             |  table_name text primary key,
             |  created timestamp not null default CURRENT_TIMESTAMP,
             |  lastPing timestamp default NULL
             |);
          """.stripMargin
        statement.executeUpdate(createTableRegister.toString)
        val tablesToDelete = statement.executeQuery(s"""SELECT table_name FROM $linkingTableRegistry""")
        while (tablesToDelete.next()) {
          val tableName = tablesToDelete.getString("table_name")
          statement.executeUpdate(s"""DROP TABLE IF EXISTS $tableName;""")
        }
        log.info("Finished clean up and initializing linking tables in RDB linking backend.")
        initialized = true
      } finally {
        conn.close()
      }
    }
  }

  def uniquePrefix: String = synchronized {
    count += 1
    "linking" + count + "_"
  }

  /** Creates a new JDBC database connection */
  def createConnection(): Connection = {
    DriverManager.getConnection(
      cfg.getString(jdbcConnectionStringConfigKey),
      cfg.getString(linkingExecutionRdbUserKey),
      cfg.getString(linkingExecutionRdbPasswordKey))
  }

  final val generatedTablesTable = "linking_generated_tables"

  final val jdbcConnectionStringConfigKey = "linking.execution.rdb.jdbcConnectionString"
  final val linkingExecutionRdbUserKey = "linking.execution.rdb.user"
  final val linkingExecutionRdbPasswordKey = "linking.execution.rdb.password"
  final val linkingExecutionRdbJdbcDriverClass = "linking.execution.rdb.jdbcDriverClass"
}