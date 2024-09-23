package org.silkframework.util

import org.mockito.MockSettings
import org.mockito.Mockito.{when, mock => mockitoMock}
import org.mockito.stubbing.Answer
import org.silkframework.execution.ExecutionReport
import org.silkframework.runtime.activity.{ActivityContext, StatusHolder, ValueHolder}

import scala.reflect.ClassTag

/**
  * Mocks that are probably used in several tests.
  */
object TestMocks extends MockitoSugar {
  /** Simple mock of the activity context */
  def activityContextMock(): ActivityContext[ExecutionReport] = {
    val context = mock[ActivityContext[ExecutionReport]]
    val valueHolder = new ValueHolder[ExecutionReport](None)
    when(context.value).thenReturn(valueHolder)
    val statusMock = mock[StatusHolder];
    when(context.status).thenReturn(statusMock)
    context
  }
}


trait MockitoSugar {

  def mock[T <: AnyRef](implicit classTag: ClassTag[T]): T = {
    mockitoMock(classTag.runtimeClass.asInstanceOf[Class[T]])
  }

  def mock[T <: AnyRef](defaultAnswer: Answer[_])(implicit classTag: ClassTag[T]): T = {
    mockitoMock(classTag.runtimeClass.asInstanceOf[Class[T]], defaultAnswer)
  }

  def mock[T <: AnyRef](mockSettings: MockSettings)(implicit classTag: ClassTag[T]): T = {
    mockitoMock(classTag.runtimeClass.asInstanceOf[Class[T]], mockSettings)
  }

  def mock[T <: AnyRef](name: String)(implicit classTag: ClassTag[T]): T = {
    mockitoMock(classTag.runtimeClass.asInstanceOf[Class[T]], name)
  }
}