package org.silkframework.entity.metadata

import org.silkframework.runtime.serialization.{ReadContext, WriteContext}

import scala.xml.{Elem, Node}

case class FailureSerializer() extends XmlMetadata[Throwable] {
  override def read(value: Node)(implicit readContext: ReadContext): Throwable = new Exception("works")

  override def write(ex: Throwable)(implicit writeContext: WriteContext[Node]): Node = writeException(ex)

  private def writeException(ex: Throwable): Node ={
    <Exception>
      <Message>{ex.getMessage}</Message>
      <Cause>{if(ex.getCause != null) writeException(ex.getCause)}</Cause>
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
        <NativeMethod>{ste.isNativeMethod}</NativeMethod>
      </STE>

    }
  }

  /**
    * The identifier used to define metadata objects in the map of [[EntityMetadata]]
    */
  override val metadataId: String = FailureSerializer.ID
}

object FailureSerializer{
  val ID: String = EntityMetadata.FAILURE_KEY
}