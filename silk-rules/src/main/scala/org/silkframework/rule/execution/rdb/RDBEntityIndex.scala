package org.silkframework.rule.execution.rdb

import java.sql._
import java.util.logging.Logger

import org.silkframework.config.DefaultConfig
import org.silkframework.dataset.DataSource
import org.silkframework.entity.{Entity, EntitySchema, TypedPath}
import org.silkframework.rule._
import org.silkframework.runtime.activity.{Activity, ActivityContext, UserContext}
import org.silkframework.util.{DPair, Identifier}
import RDBEntityIndex.{executeUpdate, executeQuery}

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
    val missingConfigs = RDBEntityIndex.missingConfigs()
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
    RDBEntityIndex.init()
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
  final val BULK_UPDATE_SIZE = 1000
  private val tablePrefix = uniquePrefix + (if(sourceOrTarget) "source_" else "target_")
  private val mainIndexTableName = tablePrefix + "mainIndexTable"
  private val entityIdColumn = "entityId"
  private val indexValueColumn = "indexValue"
  private val log: Logger = Logger.getLogger(this.getClass.getName)

  def separateIndexTable(id: String): String = tablePrefix + "separateIndexTable_" + id
  def entitySchema: EntitySchema = if(sourceOrTarget) linkSpec.entityDescriptions.source else linkSpec.entityDescriptions.target
  def linkageRule: LinkageRule = linkSpec.rule
  val booleanLinkageRule: BooleanLinkageRule = BooleanLinkageRule(linkageRule).getOrElse(
    throw new IllegalArgumentException("Linkage rule is no boolean linkage rule."))

  override def run(context: ActivityContext[EntityTables])
                  (implicit userContext: UserContext): Unit = {
    val indexProfile = profileEntities(entitySchema)
    implicit val connection: Connection = RDBEntityIndex.createConnection()
    val (forMainIndexTable, separateIndexTables) = indexProfile.indexProfiles.partition(_._2.cardinality._2 <= 1)
    // Create main index table
    try {
      createMainIndexTable(forMainIndexTable)
      createSeparateIndexTable(separateIndexTables)
      loadTables(indexProfile)
    } catch {
      case ex: SQLException =>
        throw new RuntimeException("Tables for entity linking could not be created because of SQL error. Details: " + ex.getMessage, ex)
      case ex: SQLTimeoutException =>
        throw new RuntimeException("Tables for entity linking could not be created because of SQL timeout. Details: " + ex.getMessage, ex)
    } finally {
      connection.close()
    }
  }

  def loadTables(indexProfile: RdbIndexProfile)
                (implicit userContext: UserContext,
                 connection: Connection): Unit = {
    val entities = retrieveEntities()
    val (forMainIndexTable, separateIndexTables) = indexProfile.indexProfiles.partition(_._2.cardinality._2 <= 1)
    val pathProfiles = entitySchema.typedPaths.map(tp => (tp, EntityPathProfile())).toMap
    val indexProfiles = booleanLinkageRule.comparisons.map(c => (c.id, IndexProfile())).toMap
    var entityId = 1
    val mainTableColumns = forMainIndexTable.toSeq.map(_._1.toString)
    val bulkUpdater = BulkUpdater(BULK_UPDATE_SIZE)
    for(entity <- entities) {
      val linkageRuleIndex = LinkageRuleIndex.apply(entity, booleanLinkageRule, sourceOrTarget)
      if(mainTableColumns.nonEmpty) {
        val query = s"INSERT INTO $mainIndexTableName ($entityIdColumn, ${mainTableColumns.mkString(",")}) " +
            s"VALUES ($entityId, ${linkageRuleIndex.comparisonIndexValues(mainTableColumns).map(_.headOption.map(_.toString).getOrElse("null")).mkString(",")});"
        bulkUpdater.addQueryForExecution(query)
      }
      val separateIndexTableComparisons = separateIndexTables.toSeq.map(_._1.toString)
      for ((sepIndexTable, sepIndexTableValues) <- separateIndexTableComparisons.zip(linkageRuleIndex.comparisonIndexValues(separateIndexTableComparisons));
           indexValue <- sepIndexTableValues) {
        val query = s"INSERT INTO ${separateIndexTable(sepIndexTable)} ($entityIdColumn, $indexValueColumn) VALUES ($entityId, $indexValue);"
        bulkUpdater.addQueryForExecution(query)
      }
      entityId += 1
    }
    bulkUpdater.execute()
    RdbIndexProfile(pathProfiles, indexProfiles)
  }

  case class BulkUpdater(bulkSize: Int) {
    private var queries = new StringBuilder()
    private var count = 0

    private def reset(): Unit = {
      queries = new StringBuilder()
      count = 0
    }

    def addQueryForExecution(query: String)
                            (implicit connection: Connection): Unit = {
      if(count >= bulkSize) {
        execute()
      }
      queries.append(query).append("\n")
    }

    def execute()
               (implicit connection: Connection): Unit = {
      if(count >= 0) {
        executeUpdate(queries.toString)
        reset()
      }
    }
  }

  private def createSeparateIndexTable(separateIndexTables: Map[Identifier, IndexProfile])
                                      (implicit connection: Connection): Unit = {
    for((id, _) <- separateIndexTables) {
      val createTable = new StringBuilder(s"""CREATE TABLE ${separateIndexTable(id)}(\n""")
      createTable.append(s" $entityIdColumn integer NOT NULL,\n")
      createTable.append(s" $indexValueColumn integer NOT NULL\n);")
      executeUpdate(createTable.toString())
      registerTable(separateIndexTable(id))
      log.fine("Created RDB separate entity index table: " + id)
    }
  }

  private def createMainIndexTable(forMainIndexTable: Map[Identifier, IndexProfile])
                                  (implicit connection: Connection): Unit = {
    if (forMainIndexTable.nonEmpty) {
      val createTable = new StringBuilder(s"""CREATE TABLE $mainIndexTableName(\n""")
      createTable.append(s" $entityIdColumn integer NOT NULL")
      for ((comparisonId, indexProfile) <- forMainIndexTable) {
        createTable.append(s",\n  $comparisonId integer ${if (indexProfile.cardinality._1 == 1) "NOT NULL" else ""}")
      }
      createTable.append("\n);")
      RDBEntityIndex.logSqlErrors(createTable.toString()) {
        connection.createStatement().executeUpdate(createTable.toString())
      }
      registerTable(mainIndexTableName)
      log.fine("Created RDB main entity index table: " + mainIndexTableName)
    }
  }

  private def registerTable(tableName: String)(implicit connection: Connection): Unit = {
    val query = s"""INSERT INTO ${RDBEntityIndex.linkingTableRegistry} (table_name) VALUES ('$tableName')""".stripMargin
    executeUpdate(query)
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

  private var initialized = !configured() // Do not initialize if not configured

  /** startTimeStamp and count are used to create unique execution IDs */
  private lazy val startTimeStamp: Long = System.currentTimeMillis()
  var count = 1L

  /** Clean up old tables, create missing tables. */
  def init(): Unit = synchronized {
    if(!initialized) {
      implicit val conn = createConnection()
      try {
        val createTableRegister = // TODO: IF NOT EXISTS needs Postgres 9.1, is this Ok? This exists for ages in MySQL.
          s"""CREATE TABLE IF NOT EXISTS $linkingTableRegistry(
             |  table_name text primary key,
             |  created timestamp not null default CURRENT_TIMESTAMP,
             |  lastPing timestamp not null default CURRENT_TIMESTAMP
             |);
          """.stripMargin
        executeUpdate(createTableRegister)
        val tablesToDelete = executeQuery(s"""SELECT table_name FROM $linkingTableRegistry""")
        while (tablesToDelete.next()) {
          val tableName = tablesToDelete.getString("table_name")
          val dropTableSql = s"""DROP TABLE IF EXISTS $tableName;"""
          executeUpdate(dropTableSql)
          val deleteQuery = s"delete from $linkingTableRegistry where table_name='$tableName';"
          executeUpdate(deleteQuery)
        }
        log.info("Finished clean up and initializing linking tables in RDB linking backend.")
        initialized = true
      } finally {
        conn.close()
      }
    }
  }

  def logSqlErrors[T](query: => String)
                     (block: => T): T = {
    try {
      block
    } catch {
      case ex: SQLException =>
        log.warning("There was an SQL error when executing following query: " + query)
        throw ex
    }
  }

  def configured(): Boolean = {
    missingConfigs().isEmpty
  }

  def missingConfigs(): Seq[String] = {
    Seq(
      RDBEntityIndex.jdbcConnectionStringConfigKey,
      RDBEntityIndex.linkingExecutionRdbUserKey,
      RDBEntityIndex.linkingExecutionRdbPasswordKey,
      RDBEntityIndex.linkingExecutionRdbJdbcDriverClass
    ).filter(!cfg.hasPath(_))
  }

  def uniquePrefix: String = synchronized {
    count += 1
    "linking" + startTimeStamp + "_" + count + "_"
  }

  /** Executes a query. */
  def executeUpdate(query: String)
                           (implicit connection: Connection): Int = {
    RDBEntityIndex.logSqlErrors(query) {
      val statement = connection.createStatement()
      val result = statement.executeUpdate(query)
      statement.close()
      result
    }
  }

  def executeQuery(query: String)
                          (implicit connection: Connection): ResultSet = {
    RDBEntityIndex.logSqlErrors(query) {
      val statement = connection.createStatement()
      statement.executeQuery(query)
    }
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