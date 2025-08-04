package org.silkframework.runtime.resource

import com.typesafe.config.Config
import org.silkframework.config.ConfigValue
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.resource.Resource.maxInMemorySizeParameterName

import java.io.{ByteArrayOutputStream, File, InputStream}
import java.time.Instant
import java.util.logging.Logger
import scala.io.{Codec, Source}

/**
 * A resource, such as a file.
 */
trait Resource {

  protected lazy val log: Logger = Logger.getLogger(getClass.getName)

  /**
   * The local name of this resource.
   */
  def name: String

  /**
   * The path of this resource.
   */
  def path: String

  /**
   * If the resource is part of a compressed archive, this is the path to the entry within the archive.
   */
  def entryPath: Option[String] = None

  /**
    * Checks if this resource exists.
    */
  def exists: Boolean

  /**
    * Returns the size of this resource in bytes.
    * Returns None if the size is not known.
    */
  def size: Option[Long]

  /**
    * The time that the resource was last modified.
    * Returns None if the time is not known.
    */
  def modificationTime: Option[Instant]

  /**
   * Creates an input stream for reading the resource.
   *
   * @return An input stream for reading the resource.
   *         The caller is responsible for closing the stream after reading.
   */
  def inputStream: InputStream

  /**
    * Reads the input stream with a provided read function.
    * This method should usually be preferred over requesting an inputStream as it takes care of closing the stream after reading is done.
    */
  def read[T](reader: InputStream => T): T = {
    val is = inputStream
    try {
      reader(is)
    } finally {
      is.close()
    }
  }

  /**
   * Loads this resource into a string.
   */
  def loadAsString(codec: Codec = Codec.UTF8): String = {
    checkSizeForInMemory()
    val source = Source.fromInputStream(inputStream)(codec)
    try {
      source.getLines().mkString("\n")
    } finally {
      source.close()
    }
  }

  /**
    * Loads all lines of this resource into a sequence.
    */
  def loadLines(codec: Codec = Codec.UTF8): Seq[String] = {
    checkSizeForInMemory()
    val source = Source.fromInputStream(inputStream)(codec)
    try {
      source.getLines().toList
    } finally {
      source.close()
    }
  }

  /**
    * Loads this resource into a byte array.
    */
  def loadAsBytes: Array[Byte] = {
    checkSizeForInMemory()
    val in = inputStream
    try {
      val out = new ByteArrayOutputStream()
      var b = in.read()
      while (b > -1) {
        out.write(b)
        b = in.read()
      }
      out.toByteArray
    } finally {
      in.close()
    }
  }

  /**
    * True, if this resource does exist and is not empty.
    * False, otherwise.
    */
  def nonEmpty: Boolean = {
    if(exists) {
      size match {
        case Some(s) =>
          s > 0
        case None =>
          val in = inputStream
          try {
            in.read() != -1
          } finally {
            in.close()
          }
      }
    } else {
      false
    }
  }

  /**
   * Returns the name of this resource.
   */
  override def toString: String = name

  /**
    * Checks if this resource is not too large to be loaded into memory.
    * Called by all methods that load the resource contents into memory.
    *
    * @throws ResourceTooLargeException If this resource is too large to be loaded into memory.
    */
  def checkSizeForInMemory(): Unit = {
    size match {
      case Some(s) =>
        if(s > Resource.maxInMemorySize()) {
          throw new ResourceTooLargeException(s"Resource $name is too large to be loaded into memory (size: $s, maximum size: ${Resource.maxInMemorySize()}). " +
            s"Configure '$maxInMemorySizeParameterName' in order to increase this limit.")
        }
      case None =>
        log.warning(s"Could not determine size of resource $name for loading contents into memory.")
    }
  }

  /**
   * The relative path within a resource manager.
   *
   * @throws IllegalArgumentException If the given resource manager is either empty or
   *                                  does have a different base path than this resource.
   */
  def relativePath(resourceManager: ResourceManager): String = {
    if (resourceManager == EmptyResourceManager()) {
      throw new IllegalArgumentException("Need non-empty resource manager in order to serialize resource paths relative to base path.")
    }
    val basePath = resourceManager.basePath
    if (path.startsWith(basePath)) {
      path.stripPrefix(basePath).stripPrefix("/").stripPrefix(File.separator)
    } else {
      throw new IllegalArgumentException("The context uses a different base path than the provided resource.")
    }
  }
}

object Resource {

  final val maxInMemorySizeParameterName = s"${classOf[Resource].getName}.maxInMemorySize"

  final val freeSpaceThresholdParameterName = s"${classOf[Resource].getName}.minDiskSpace"

  /**
    * Maximum resource size in bytes that should be loaded into memory.
    */
  val maxInMemorySize: ConfigValue[Long] = (config: Config) => {
    config.getMemorySize(maxInMemorySizeParameterName).toBytes
  }

  /**
    * Minimum amount of free space before files to the local file system are written.
    */
  val freeSpaceThreshold: ConfigValue[Option[Long]] = (config: Config) => {
    if(!config.hasPath(freeSpaceThresholdParameterName)) {
      None
    } else {
      Some(config.getMemorySize(freeSpaceThresholdParameterName).toBytes)
    }
  }

  /**
    * Checks if there is enough free space left on the file system the file resides on.
    *
    * @throws NotEnoughDiskSpaceException If there is not enough space left.
    **/
  def checkFreeSpace(file: File): Unit = {
    freeSpaceThreshold() foreach { limit =>
      def checkRecursive(file: File): Unit = {
        // We can only get FS stats from existing files, so we need to recursively go up until a parent directory exists
        if (!file.exists()) {
          Option(file.getParentFile).foreach(parent => checkFreeSpace(parent))
        } else {
          val freeSpace = file.getUsableSpace
          if (freeSpace < limit) {
            throw new NotEnoughDiskSpaceException(s"Cannot write to file '${file.getName}'. Free space of $freeSpace is less than the configured" +
              s" minimal value of $limit. You can change the threshold via " +
              s"config parameter '${Resource.freeSpaceThresholdParameterName}'.")
          }
        }
      }
      checkRecursive(file.getAbsoluteFile)
    }
  }
}