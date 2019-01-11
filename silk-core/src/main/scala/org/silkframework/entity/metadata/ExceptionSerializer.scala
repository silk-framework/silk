package org.silkframework.entity.metadata

import java.lang.reflect.Constructor

import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
import ExceptionSerializer._
import org.slf4j.LoggerFactory

import scala.xml.{Elem, Node}

/**
  * XML serializer for exceptions
  * NOTE: use [[readException]] and [[write]] to reference more specific Serializers for subclasses of Throwable
  */
case class ExceptionSerializer() extends XmlMetadataSerializer[Throwable] {

  @transient
  private val logger = LoggerFactory.getLogger(this.getClass)

  override def read(ex: Node)(implicit readContext: ReadContext): Throwable = readException(ex)

  def readException(node: Node): Throwable = {

    if(node == null || node.text.trim.isEmpty) {
      return null
    }

    val classOpt = getExceptionClassOption(node)
    val exceptionClass = classOpt.getOrElse( new UnknownError("Exception without an associated class").getClass.asInstanceOf[Class[Throwable]])

    //FIXME introduce an automated registry for this switch?
    exceptionClass match{
      //NOTE: insert special Exception reading switch here
      //case ex: SpecialException => readSpecialException(..)
      case _ => readDefaultThrowable(node, exceptionClass)
    }
  }

  /**
    * Finds a fitting constructor and returns the instantiated exception object.
    *
    * @param node - the XML node
    * @param exceptionClass - the exception class in question
    * @return
    */
  def readDefaultThrowable(node: Node, exceptionClass: Class[Throwable]): Throwable = {
    val messageOpt = Option((node \ MESSAGE).text.trim)
    val causeOpt = getExceptionCauseOption(node)
    val exceptionClassOpt = getExceptionClassOption(node)
    val constructorAndArguments = getExceptionConstructorOption(causeOpt, exceptionClassOpt, messageOpt, exceptionClass.getSimpleName)

    val exception = if (constructorAndArguments.nonEmpty) {
      val constructorAndArguments = getExceptionConstructorOption(causeOpt, exceptionClassOpt, messageOpt, exceptionClass.getSimpleName)
      constructorAndArguments.get._1.newInstance(constructorAndArguments.get._2: _*)
    }
    else {
      new Exception(
        s"Emulated Exception of class: ${exceptionClass.getCanonicalName} original message: ${messageOpt.orNull}",
        causeOpt.orNull
      )
    }

    if ((node \ STACKTRACE).nonEmpty) exception.setStackTrace(readStackTrace((node \ STACKTRACE).head))
    exception
  }


  /**
    * Method to help retrieve optional causes.
    *
    * @param node XML node
    * @return
    */
  private def getExceptionCauseOption(node: Node): Option[Throwable] = {
    val cause = readException((node \ CAUSE).headOption.flatMap(_.child.headOption).orNull)
    Option(cause)
  }

  /**
    * Method to help retrieve the exception class, which should not be empty but apparently is sometimes.
    *
    * @param node XML node
    * @return
    */
  private def getExceptionClassOption(node: Node): Option[Class[Throwable]] = {
    val className = (node \ CLASS).text
    try {
      if (className.isEmpty) {
        None
      }
      else {
        val c = java.lang.Class.forName(className)
        Some(c.asInstanceOf[Class[Throwable]])
      }
    }
    catch {
      case _: Throwable =>
        logger.error(s"The raised exception object does not exist as a known class and can't be serialized: $className")
        None
    }
  }

  /**
    * Determines the constructor of a Throwable and its arguments as an object set.
    * Depending on the input, different constructors a searched via reflection.
    * A (String, Throwable) constructor is preferred, after that (Throwable, String), (Throwable), (String)
    * and finally a no argument constructor.
    *
    * Available parameters are secondary to the order. That means even if there is
    * no cause we prefer the (Throwable, String) constructor with an empty Throwable.
    *
    * @param cause            Throwable
    * @param exceptionClass   Class
    * @param message          Message
    * @param className        className
    * @return Constructor of a Throwable and its parameters or None
    */
  def getExceptionConstructorOption(cause: Option[Throwable], exceptionClass: Option[Class[Throwable]], message: Option[String],
                              className: String): Option[(Constructor[Throwable], Seq[Object])] = {
    try {
      val candidates = exceptionClass.get.getConstructors.filter(
        c => c.getParameterTypes.contains(classOf[String]) || c.getParameterTypes.contains(classOf[Throwable])
      ).sortWith( (c1,c2) => c1.getParameterCount > c2.getParameterCount)

      candidates.map(_.getParameterTypes).head match {
        case Array(_, _) =>
          try {
            val constructor: Constructor[Throwable] = exceptionClass.get.getConstructor(classOf[String], classOf[Throwable])
            val args: Seq[Object] = Seq(message.orNull, cause.orNull)
            Some((constructor, args))
          }
          catch {
            case _: NoSuchElementException =>
              val constructor: Constructor[Throwable] = exceptionClass.get.getConstructor(classOf[Throwable], classOf[String])
              val args: Seq[Object]  = Seq(cause.orNull, message.orNull)
              Some((constructor, args))
            case _: Throwable =>
              None
          }
        case Array(_) =>
          try {
            val constructor: Constructor[Throwable] = exceptionClass.get.getConstructor(classOf[Throwable])
            val args = Seq(cause.orNull)
            Some((constructor, args))
          }
          catch {
            case _:NoSuchElementException =>
              val constructor: Constructor[Throwable] = exceptionClass.get.getConstructor(classOf[String])
              val args = Seq(message.orNull)
              Some(constructor, args)
            case _: Throwable => None
          }
        case Array() =>
          val constructor: Constructor[Throwable] = exceptionClass.get.getConstructor()
          val args = Seq()
          Some(constructor, args)
        case _ =>
          None
      }
    }
    catch {
      case _: java.lang.IllegalArgumentException =>
        logger.warn(s"A constructor for the exception: $className could not be found because of the given arguments")
        None
      case _: Throwable =>
        throw new RuntimeException(s"Construction of exception $className failed for unknown reasons")
    }
  }



  def readStackTrace(node: Node): Array[StackTraceElement] ={
    val stackTrace = for(ste <- node \ STELEMENT) yield{
      val className = (ste \ CLASSNAME).text.trim
      val methodName = (ste \ METHODNAME).text.trim
      val fileName = (ste \ FILENAME).text.trim
      val lineNumber = (ste \ LINENUMBER).text.trim
      // TODO please review: I did not change from 0 to -1. That seems better, though.
      new StackTraceElement(className, methodName, fileName, if(lineNumber.length > 0) lineNumber.toInt else -1)
    }
    stackTrace.toArray
  }

  override def write(ex: Throwable)(implicit writeContext: WriteContext[Node]): Node = ex.getClass match{
    //FIXME introduce an automated registry for this switch?
  //case se: SpecialException => writeSpecialException(..)
    case _ => writeException(ex)
  }

  private def writeException(ex: Throwable): Node ={
    <Exception>
      <Class>{ex.getClass.getCanonicalName}</Class>
      <Message>{ex.getMessage}</Message>
      <Cause>
        {if(ex.getCause != null) writeException(ex.getCause)}
      </Cause>
      <StackTrace>
        {writeStackTrace(ex)}
      </StackTrace>
    </Exception>
  }

  private def writeStackTrace(ex: Throwable): Array[Elem] ={
    for(ste <- ex.getStackTrace) yield{
      <STE>
        <FileName>{ste.getFileName}</FileName>
        <ClassName>{ste.getClassName}</ClassName>
        <MethodName>{ste.getMethodName}</MethodName>
        <LineNumber>{ste.getLineNumber}</LineNumber>
      </STE>
    }
  }

  /**
    * The identifier used to define metadata objects in the map of [[EntityMetadata]]
    */
  override def metadataId: String = ExceptionSerializer.ID

  /**
    * An indicator whether the LazyMetadata object produced with this serializer will be replaceable (overridable in the Metadata map)
    *
    * @return
    */
  override def replaceableMetadata: Boolean = false
}

object ExceptionSerializer{
  val ID: String = "exception_class"
  val EXCEPTION: String = "Exception"
  val CLASS: String = "Class"
  val MESSAGE: String = "Message"
  val CAUSE: String = "Cause"
  val STACKTRACE: String = "StackTrace"
  val STELEMENT: String = "STE"
  val FILENAME: String = "FileName"
  val CLASSNAME: String = "ClassName"
  val METHODNAME: String = "MethodName"
  val LINENUMBER: String = "LineNumber"
}
