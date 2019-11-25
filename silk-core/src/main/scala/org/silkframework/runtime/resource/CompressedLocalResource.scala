package org.silkframework.runtime.resource
import java.io.{ByteArrayInputStream, ByteArrayOutputStream, File, FileInputStream, FileOutputStream, InputStream, OutputStream}
import java.time.Instant

import net.jpountz.lz4.{LZ4BlockInputStream, LZ4BlockOutputStream, LZ4Factory, LZ4FrameInputStream, LZ4FrameOutputStream}

/**
  * A resource that's held in-memory and is being compressed.
  */
case class CompressedInMemoryResource(name: String, path: String, knownTypes: IndexedSeq[String]) extends WritableResource with ResourceWithKnownTypes {

  @volatile
  private var byteArrays: Vector[Array[Byte]] = Vector.empty

  override def write(append: Boolean)(write: OutputStream => Unit): Unit = {
    val byteArrayOS = new ByteArrayOutputStream()
    val compressedOS = new LZ4BlockOutputStream(byteArrayOS)
    write(compressedOS)
    compressedOS.flush()
    compressedOS.close()
    if(append) {
      byteArrays :+= byteArrayOS.toByteArray
    } else {
      byteArrays = Vector(byteArrayOS.toByteArray)
    }
  }

  override def delete(): Unit = {
    byteArrays = Vector.empty
  }

  override def exists: Boolean = byteArrays.nonEmpty

  override def size: Option[Long] = Some(byteArrays.map(_.length).sum)

  override def modificationTime: Option[Instant] = None

  override def inputStream: InputStream = {
    CompressedMultiByteArraysInputStream(byteArrays)
  }
}

/** An input stream that iterates over multiple compressed byte arrays. */
case class CompressedMultiByteArraysInputStream(byteArrays: IndexedSeq[Array[Byte]]) extends InputStream {
  private var currentIdx = 0
  private var isOpt: Option[InputStream] = None

  override def read(): Int = {
    var returnValue = -1
    while(returnValue == -1 && currentIdx < byteArrays.length) {
      isOpt match {
        case Some(is) =>
          val nextRead = is.read()
          if(nextRead == -1) {
            isOpt = None
            currentIdx += 1
          } else {
            returnValue = nextRead
          }
        case None =>
          isOpt = Some(new LZ4BlockInputStream(new ByteArrayInputStream(byteArrays(currentIdx))))
      }
    }
    returnValue
  }
}

/**
  * Resource that is held in a compressed file.
 *
  * @param file The file that holds the compressed resource data.
  * @param name The name of the resource.
  * @param path The path of the resource.
  * @param knownTypes Known types that should be returned, leave empty if types should be automatically determined.
  * @param deleteOnGC Deletes the given file when the resource object is garbage collected.
  *                   This should be used when the file has no use after the resource gets garbage collected and
  *                   it cannot be determined when to delete before the GC.
  */
case class CompressedFileResource(file: File, name: String, path: String, knownTypes: IndexedSeq[String], deleteOnGC: Boolean)
    extends WritableResource
    with ResourceWithKnownTypes
    with DeleteUnderlyingResourceOnGC {
  override def write(append: Boolean)(write: OutputStream => Unit): Unit = {
    val os = outputStream(append)
    write(os)
    os.flush()
    os.close()
  }

  override def delete(): Unit = {
    file.delete()
  }

  override def modificationTime: Option[Instant] = Some(Instant.ofEpochMilli(file.lastModified()))

  override def inputStream: InputStream = {
    if(exists) {
      new LZ4FrameInputStream(new FileInputStream(file))
    } else {
      new ByteArrayInputStream(Array.empty[Byte])
    }
  }

  private def outputStream(append: Boolean): OutputStream = {
    new LZ4FrameOutputStream(new FileOutputStream(file, append))
  }

  override def exists: Boolean = file.exists()

  override def size: Option[Long] = Some(file.length())
}

/**
  * A compression wrapper for any given resource
  */
case class CompressedResourceWrapper(res: WritableResource) extends WritableResource with ResourceWithKnownTypes{
  /**
    * Preferred method for writing to a resource.
    *
    * @param write A function that accepts an output stream and writes to it.
    */
  override def write(append: Boolean)(write: OutputStream => Unit): Unit = {
    val bytes = new ByteArrayOutputStream()
    val l4z = new LZ4FrameOutputStream(bytes)
    write(l4z)
    res.writeBytes(bytes.toByteArray, append)
  }

  /**
    * Deletes this resource.
    */
  override def delete(): Unit = res.delete()

  override def knownTypes: IndexedSeq[String] = res match{
    case rwkt: ResourceWithKnownTypes => rwkt.knownTypes
    case _ => IndexedSeq.empty
  }

  /**
    * The local name of this resource.
    */
  override def name: String = res.name

  /**
    * The path of this resource.
    */
  override def path: String = res.path

  /**
    * Checks if this resource exists.
    */
  override def exists: Boolean = res.exists

  /**
    * Returns the size of this resource in bytes.
    * Returns None if the size is not known.
    */
  override def size: Option[Long] = res.size

  /**
    * The time that the resource was last modified.
    * Returns None if the time is not known.
    */
  override def modificationTime: Option[Instant] = res.modificationTime

  /**
    * Creates an input stream for reading the resource.
    *
    * @return An input stream for reading the resource.
    *         The caller is responsible for closing the stream after reading.
    */
  override def inputStream: InputStream = {
    if(exists) {
      new LZ4FrameInputStream(res.inputStream)
    } else {
      new ByteArrayInputStream(Array.empty[Byte])
    }
  }
}
