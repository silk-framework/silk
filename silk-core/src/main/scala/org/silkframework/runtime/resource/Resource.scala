package org.silkframework.runtime.resource

import com.typesafe.config.Config
import org.silkframework.config.ConfigValue
import org.silkframework.runtime.plugin.PluginContext

import java.io.{File, InputStream, InputStreamReader}
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
    * The local file holding this resource's content as-is, if there is one.
    */
  def underlyingFile: Option[File] = None

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
   * Loads at most maxChars characters of this resource into a string.
   * The read is bounded, so it is safe for resources over the in-memory size limit.
   * Unlike [[loadAsString]], line endings are kept verbatim.
   */
  def loadAsStringCapped(maxChars: Int, codec: Codec = Codec.UTF8): CappedString = {
    require(maxChars >= 0 && maxChars < Int.MaxValue, "maxChars must be non-negative and below Int.MaxValue")
    read { is =>
      val reader = new InputStreamReader(is, codec.charSet)
      val buffer = new Array[Char](maxChars + 1)
      var total = 0
      var n = 0
      while (n != -1 && total < buffer.length) {
        n = reader.read(buffer, total, buffer.length - total)
        if (n > 0) total += n
      }
      if (total > maxChars) CappedString(new String(buffer, 0, maxChars), truncated = true)
      else CappedString(new String(buffer, 0, total), truncated = false)
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
      in.readAllBytes()
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
          throw new ResourceTooLargeException(name, s)
        }
      case None =>
        log.warning(s"Could not determine size of resource $name for loading contents into memory.")
    }
  }

  /**
   * The relative path within a resource manager.
   * Always uses '/' as separator, independent of the OS, so it can be resolved via [[ResourceManager.getInPath]].
   *
   * @throws IllegalArgumentException If the given resource manager is either empty or
   *                                  does have a different base path than this resource.
   */
  def relativePath(resourceManager: ResourceManager): String = {
    relativePath(resourceManager, File.separatorChar)
  }

  // The OS separator is a parameter so the Windows normalization can be tested on any OS
  private[resource] def relativePath(resourceManager: ResourceManager, separatorChar: Char): String = {
    if (resourceManager == EmptyResourceManager()) {
      throw new IllegalArgumentException("Need non-empty resource manager in order to serialize resource paths relative to base path.")
    }
    val separator = separatorChar.toString
    val basePath = resourceManager.basePath
    val relative = path.stripPrefix(basePath)
    // The remainder must start at a separator, so that e.g. base path '/proj' does not match '/proj2/file'
    if (path.startsWith(basePath) && (relative.isEmpty || relative.startsWith("/") || relative.startsWith(separator))) {
      relative.stripPrefix("/").stripPrefix(separator).replace(separatorChar, '/')
    } else {
      throw new IllegalArgumentException("The context uses a different base path than the provided resource.")
    }
  }
}

/**
  * A string loaded with a character bound, as returned by [[Resource.loadAsStringCapped]].
  *
  * @param content   The loaded, possibly truncated, content.
  * @param truncated True if the source was longer than the bound and content was cut off.
  */
case class CappedString(content: String, truncated: Boolean)

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