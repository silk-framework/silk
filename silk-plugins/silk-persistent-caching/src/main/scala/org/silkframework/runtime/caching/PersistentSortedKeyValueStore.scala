package org.silkframework.runtime.caching

import com.typesafe.config.Config
import org.lmdbjava.DbiFlags.MDB_CREATE
import org.lmdbjava.Env.create
import org.lmdbjava._
import org.silkframework.config.{ConfigValue, DefaultConfig}
import org.silkframework.runtime.caching.PersistentSortedKeyValueStore.byteBufferToString
import org.silkframework.runtime.iterator.CloseableIterator
import org.silkframework.util.FileUtils.toFileUtils
import org.silkframework.util.Identifier

import java.io.{File, IOException}
import java.nio.{Buffer, ByteBuffer}
import java.nio.ByteBuffer.allocateDirect
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import java.util
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.{Level, Logger}
import scala.util.{Failure, Success, Try};

/**
  * A file system backed ordered key value store containing a single DB.
  *
  *
  * @param databaseId          System-wide unique identifier for the database.
  * @param optionalDbDirectory If defined this directory will be used to store the DB, else a directory will be created/used
  *                            via a combination of the configured cache directory and DB ID.
  * @param temporary           If enabled the files will be located in a temporary directory that is cleared automatically
  *                            on every start. This makes sure that files that were not deleted properly, e.g. because of a crash,
  *                            will be removed at every start.
  *                            If an optional DB directory is chosen, this flag will be ignored.
  * @param config              Config parameters.
  */
case class PersistentSortedKeyValueStore(databaseId: Identifier,
                                         optionalDbDirectory: Option[File] = None,
                                         temporary: Boolean = false,
                                         config: PersistentSortedKeyValueStoreConfig = PersistentSortedKeyValueStoreConfig(
                                           tooLargeKeyStrategy = HandleTooLargeKeyStrategy.TruncateKeyWithHash)
                                        ) {
  private val log: Logger = Logger.getLogger(this.getClass.getCanonicalName)
  private lazy val dbDirectory: File = {
    val dir = optionalDbDirectory.getOrElse {
      if(temporary) {
        new File(PersistentSortedKeyValueStore.tempCacheDirectory, databaseId)
      } else {
        new File(PersistentSortedKeyValueStore.cacheDirectory, databaseId)
      }
    }
    dir.safeMkdirs()
    dir
  }
  @volatile
  private var env: Env[ByteBuffer] = createEnv()
  @volatile
  private var db: Dbi[ByteBuffer] = openDB()
  private val initialized = new AtomicBoolean(true)

  /** The maximum key size this store is able to store. */
  val maxKeySize: Int = env.getMaxKeySize

  private def openDB(): Dbi[ByteBuffer] = {
    env.openDbi(databaseId, MDB_CREATE)
  }

  private def createEnv(): Env[ByteBuffer] = {
    Thread.currentThread().setContextClassLoader(getClass.getClassLoader)
    create()
        .setMapSize(config.maxSizeInBytes)
        .setMaxDbs(1)
        .open(dbDirectory, EnvFlags.MDB_NOSYNC, EnvFlags.MDB_NOTLS)
  }

  /** The number of entries in the store. */
  def size(): Long = withReadTransaction { txn =>
    db.stat(txn).entries
  }

  /** Create a key buffer from a String value. */
  def createKeyBuffer(key: String): ByteBuffer = {
    createKeyBuffer(key.getBytes(UTF_8))
  }

  private def byteArrayToString(byteArray: Array[Byte]): String = {
    Try(byteBufferToString(ByteBuffer.wrap(byteArray))).getOrElse("- cannot decode bytes -")
  }

  /** Create key buffer from an array. */
  def createKeyBuffer(key: Array[Byte]): ByteBuffer = {
    var keyBytes = key
    if(config.compressKeys) {
      keyBytes = CompressionHelper.lz4Compress(keyBytes, addLengthPreamble = true)
    }
    if(keyBytes.length > maxKeySize) {
      config.tooLargeKeyStrategy match {
        case HandleTooLargeKeyStrategy.ThrowError =>
          throw new IllegalArgumentException(s"Failed to create key for key/value store of DB '$databaseId'. Key is larger than allowed" +
            s" size of $maxKeySize. Key (${keyBytes.length} bytes): ${byteArrayToString(key)}")
        case HandleTooLargeKeyStrategy.TruncateKey =>
          log.fine(s"Key '${byteArrayToString(key)}' is longer than $maxKeySize bytes. Truncating key.")
          keyBytes = util.Arrays.copyOfRange(keyBytes, 0, maxKeySize);
        case HandleTooLargeKeyStrategy.TruncateKeyWithHash =>
          keyBytes = HandleTooLargeKeyStrategy.truncateKeyWithHash(keyBytes, maxKeySize)
      }
    }
    val keyBuffer = allocateDirect(maxKeySize)
    if(keyBytes.length > 0) {
      keyBuffer.put(keyBytes)
    } else {
      keyBuffer.put(Array(Byte.MinValue)) // TODO: How to better handle empty strings? An empty byte buffer evokes an error being raised later on.
    }
    // Casting to Buffer to avoid conflict mentioned here: https://stackoverflow.com/questions/61267495/exception-in-thread-main-java-lang-nosuchmethoderror-java-nio-bytebuffer-flip
    keyBuffer.asInstanceOf[Buffer].flip()
    keyBuffer
  }

  /** Create the value buffer that should be written to the store. */
  def createValueBuffer(value: String): ByteBuffer = {
    createValueBuffer(value.getBytes(UTF_8))
  }

  /** Create the value buffer that should be written to the store. */
  def createValueBuffer(value: Array[Byte]): ByteBuffer = {
    var valueBytes = value
    if(config.compressValues) {
      // We add the length to the beginning, so it can be efficiently decompressed.
      valueBytes = CompressionHelper.lz4Compress(valueBytes, addLengthPreamble = true)
    }
    val valueBuffer = allocateDirect(valueBytes.length)
    // Casting to Buffer to avoid conflict mentioned here: https://stackoverflow.com/questions/61267495/exception-in-thread-main-java-lang-nosuchmethoderror-java-nio-bytebuffer-flip
    valueBuffer.put(valueBytes).asInstanceOf[Buffer].flip()
    valueBuffer
  }

  /** Insert a key/value pair */
  def put(key: String, value: String, txnOpt: Option[Txn[ByteBuffer]]): Unit = {
    val keyBuffer = createKeyBuffer(key)
    val valueBuffer = createValueBuffer(value)
    put(keyBuffer, valueBuffer, txnOpt)
  }

  /** Insert a key/value pair */
  def put(key: Array[Byte], value: Array[Byte], txnOpt: Option[Txn[ByteBuffer]]): Unit = {
    put(createKeyBuffer(key), createValueBuffer(value), txnOpt)
  }

  /** Insert a key/value pair */
  def put(keyBuffer: ByteBuffer, valueBuffer: ByteBuffer, txnOpt: Option[Txn[ByteBuffer]]): Unit = {
    if(keyBuffer.remaining() > maxKeySize) {
      throw new RuntimeException(s"Trying to write key with byte size ${keyBuffer.remaining()} into persistent key value store. Max. key length: $maxKeySize")
    }
    checkState()
    txnOpt match {
      case Some(txn) => db.put(txn, keyBuffer, valueBuffer)
      case None => db.put(keyBuffer, valueBuffer)
    }
  }

  /**
    * Deletes a key/value pair
    * @return true if the key/data pair was found, false otherwise
    */
  def delete(key: String): Boolean = {
    checkState()
    val keyBuffer = createKeyBuffer(key)
    db.delete(keyBuffer)
  }

  /** Get the value of a key as String. */
  def getString(key: String, txnOpt: Option[Txn[ByteBuffer]] = None): Option[String] = {
    getBytes(key, txnOpt).map(PersistentSortedKeyValueStore.byteBufferToString)
  }

  /** Removes this store and all files on the FS. */
  def deleteStore(): Unit = {
    this.synchronized {
      checkState()
      initialized.set(false)
    }
    close()
    try {
      org.apache.commons.io.FileUtils.forceDelete(dbDirectory)
    } catch {
      case ex: IOException =>
        log.log(Level.WARNING, s"Key value cache store files could not be deleted. Store directory: ${dbDirectory.getAbsolutePath}. Cause: ", ex)
    }
  }

  /**
    * Iterator over all entries in the store.
    * @return An iterator over the stored key and the value as [[ByteBuffer]].
    *         The iterator must be closed when finished using it.
    */
  def iterateEntries(): CloseableIterator[(ByteBuffer, ByteBuffer)] = {
    val txn = env.txnRead()
    val cursor = new CursorWrapper(db.openCursor(txn))
    // LMDB's cursor supports no hasNext function, so we need to implement it on our own.
    var nextEntry: Option[(ByteBuffer, ByteBuffer)] = if(cursor.first()) {
      Some(cursorKeyValue(cursor))
    } else {
      None
    }
    new CloseableIterator[(ByteBuffer, ByteBuffer)] {
      var closed = false
      override def close(): Unit = synchronized {
        if(!closed) {
          closed = true
          txn.close()
        }
      }

      override def hasNext: Boolean = {
        nextEntry.isDefined || setNextEntry()
      }

      // LMDB's cursor has no hasNext function, so we need to look ahead and keep the next entry.
      private def setNextEntry(): Boolean = {
        nextEntry = if(!closed && cursor.next()) {
          Some(cursorKeyValue(cursor))
        } else {
          close()
          None
        }
        nextEntry.isDefined
      }

      override def next(): (ByteBuffer, ByteBuffer) = {
        nextEntry match {
          case Some(entry) =>
            nextEntry = None
            entry
          case None =>
            // first try to handle the case that next() is called multiple times without hasNext()
            if(setNextEntry()) {
              val entry = nextEntry.get
              nextEntry = None
              entry
            } else {
              throw new IllegalStateException("Called next() on empty iterator.")
            }
        }
      }
    }
  }

  /** Iterator over all entries interpreting both the key and the value as UTF-8 Strings.
    * This */
  def iterateStringEntries(): CloseableIterator[(String, String)] = {
    val byteIterator = iterateEntries()
    byteIterator.map { (keyValue: (ByteBuffer, ByteBuffer)) =>
      PersistentSortedKeyValueStore.byteBufferToString(keyValue._1) -> PersistentSortedKeyValueStore.byteBufferToString(keyValue._2)
    }
  }

  private def cursorKeyValue(cursor: CursorWrapper): (ByteBuffer, ByteBuffer) = {
    cursor.byteKey() -> cursor.byteValue()
  }

  /** Cleans all data from the store and re-initializes it. */
  def clearStore(): Unit = {
    deleteStore()
    dbDirectory.mkdirs()
    env = createEnv()
    db = openDB()
    initialized.set(true)
  }

  /** Closes the DB and frees resources. */
  def close(): Unit = {
    env.close()
  }

  private def checkState(): Unit = {
    if(!initialized.get()) {
      throw new IllegalStateException(s"The persistent key value store with database ID '$databaseId' is not initialized!")
    }
  }

  def getBytes(key: String, txnOpt: Option[Txn[ByteBuffer]]): Option[ByteBuffer] = {
    getBytes(createKeyBuffer(key), txnOpt)
  }

  def getBytes(key: ByteBuffer, txnOpt: Option[Txn[ByteBuffer]]): Option[ByteBuffer] = {
    checkState()
    var value = txnOpt match {
      case Some(txn) => db.get(txn, key)
      case None => withReadTransaction { txn => db.get(txn, key) }
    }
    if (config.compressValues && value != null) {
      value = CompressionHelper.lz4decompressByteBuffer(value)
    }
    Option(value)
  }

  /** Run code with a read transaction. Transaction is automatically closed after the code block.
    * When writing or reading many values, using the same transaction for as many actions before committing should be preferred,
    * because of much better performance. */
  def withReadTransaction[T](transactionBlock: Txn[ByteBuffer] => T): T = {
    checkState()
    var txn: Txn[ByteBuffer] = null
    try {
      txn = env.txnRead()
      transactionBlock(txn)
    } finally {
      if(txn != null) {
        txn.close()
      }
    }
  }

  /** Run code with a write transaction. Transaction is automatically closed after the code block.
    * The caller has to commit the transaction. */
  def withWriteTransaction[T](transactionBlock: Txn[ByteBuffer] => T): T = {
    checkState()
    var txn: Txn[ByteBuffer] = null
    try {
      txn = env.txnWrite()
      transactionBlock(txn)
    } finally {
      if(txn != null) {
        txn.close()
      }
    }
  }

  class CursorWrapper(cursor: Cursor[ByteBuffer]) {
    /** Puts the cursor on the first entry.
      * @return false if entry was not found */
    def first(): Boolean = cursor.first()

    /**
      * Position at next entry.
      * @return false if entry was not found
      */
    def next(): Boolean = cursor.next()

    /** put a string value entry into the store via the cursor. */
    def put(key: String, value: String): Unit = {
      val keyBuffer = createKeyBuffer(key)
      val valueBuffer = createValueBuffer(value)
      cursor.put(keyBuffer, valueBuffer)
    }

    /** Put an entry into the store via the cursor. */
    def put(key: String, value: Array[Byte]): Unit = {
      val keyBuffer = createKeyBuffer(key)
      val valueBuffer = createValueBuffer(value)
      cursor.put(keyBuffer, valueBuffer)
    }

    /** The byte value of the key the cursor is currently positioned on. */
    def byteValue(): ByteBuffer = {
      var byteValue = cursor.`val`()
      if(config.compressValues && byteValue != null) {
        byteValue = CompressionHelper.lz4decompressByteBuffer(byteValue)
      }
      byteValue
    }

    def byteKey(): ByteBuffer = {
      var keyBytes = cursor.key()
      if(config.compressKeys && keyBytes != null) {
        keyBytes = CompressionHelper.lz4decompressByteBuffer(keyBytes)
      }
      keyBytes
    }

    /** Positions the cursor on the given key.
      * Returns false if the key was not found. */
    def position(key: String): Boolean = {
      cursor.get(createKeyBuffer(key), GetOp.MDB_SET_KEY)
    }

    /** The string value from the key the cursor is currently positioned on. */
    def stringValue(): Option[String] = {
      Option(byteValue()).map(UTF_8.decode(_).toString)
    }
  }

  /** Run code with a cursor. All code will run in a single transaction.
    * If you need access to the transaction use withTransaction.
    * Transaction is committed and transaction and cursor are closed after the code block. */
  def withCursor[T](cursorBlock: CursorWrapper => T): T = {
    checkState()
    withWriteTransaction { txn =>
      var cursor: Cursor[ByteBuffer] = null
      try {
        cursor = db.openCursor(txn)
        cursorBlock(new CursorWrapper(cursor))
      } finally {
        if(cursor != null) {
          cursor.close()
          txn.commit()
        }
      }
    }
  }
}

object PersistentSortedKeyValueStore {
  private val log: Logger = Logger.getLogger(getClass.getName)

  private val cacheDirectoryConfigKey = "caches.persistence.directory"

  private val defaultDbSizeConfigKey = "caches.persistence.maxSize"

  // Init temp dir
  removeTempDirectories()

  /** The main directory where all environments/DBs will be stored. */
  def cacheDirectory: File = {
    val cfg = DefaultConfig.instance()
    val file = if(cfg.hasPathOrNull(cacheDirectoryConfigKey)) {
      new File(cfg.getString(cacheDirectoryConfigKey))
    } else {
      val tempDir = Files.createTempDirectory("tempDiCache").toFile
      log.warning(s"No persistent cache directory specified. Please set config parameter 'caches.persistence.directory' " +
          s"appropriately. Using temporary directory ${tempDir.getAbsolutePath}.")
      tempDir
    }
    if(!file.exists()) {
      file.safeMkdirs()
    }
    file
  }

  /** Directory where temporary databases are stored that will be removed on every start of the application. */
  def tempCacheDirectory: File = {
    new File(cacheDirectory, "tmp")
  }

  private def removeTempDirectories(): Unit = {
    log.info("Cleaning tmp key/value database directory.")
    try {
      org.apache.commons.io.FileUtils.deleteDirectory(tempCacheDirectory)
      tempCacheDirectory.safeMkdirs()
    } catch {
      case exception: IOException =>
        log.log(Level.WARNING, "Could not clean up temp cache dir " + tempCacheDirectory.getAbsolutePath, exception)
    }
  }

  def byteBufferToString(byteBuffer: ByteBuffer): String = UTF_8.decode(byteBuffer).toString

  /**
   * Checks if PersistentValueStore is working as expected.
   * Can be used in a boot check.
   */
  def check(): Try[Unit] = {
    try {
      val store = new PersistentSortedKeyValueStore("tempDatabase", temporary = true)
      store.close()
      Success(())
    } catch {
      case ex: Throwable =>
        // We need to catch all throwables here, because LinkageError might be thrown if LMDB does not include a library for the host OS.
        Failure(ex)
    }
  }

  /** The max. size of the DB. Does not hurt to over-estimate. Actions will fail if limit is reached. */
  final val defaultMaxSizeInBytes: ConfigValue[Long] = (config: Config) => {
    if(config.hasPath(defaultDbSizeConfigKey)) {
      config.getMemorySize(defaultDbSizeConfigKey).toBytes
    } else {
      10L * 1024 * 1024 * 1024 // 10 GB
    }
  }

  /** The max key size. This can only be changed via compile time flags in LMDB. */
  final val MAX_KEY_SIZE_BYTES = 511
}
