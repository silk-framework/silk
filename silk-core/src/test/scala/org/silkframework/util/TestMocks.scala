package org.silkframework.util

import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import org.silkframework.execution.ExecutionReport
import org.silkframework.runtime.activity.{ActivityContext, ValueHolder}

/**
  * Mocks that are probably used in several tests.
  */
object TestMocks extends MockitoSugar {
  /** Simple mock of the activity context */
  def activityContextMock(): ActivityContext[ExecutionReport] = {
    val context = mock[ActivityContext[ExecutionReport]]
    when(context.value).thenReturn(new ValueHolder[ExecutionReport](None))
    context
  }
}
