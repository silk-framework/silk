package org.silkframework.failures

import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.entity.metadata.GenericExecutionFailure

class FailureClassTest extends FlatSpec with MustMatchers {
  behavior of "FailureClass"

  private val thisClassName = this.getClass.getName

  case class SomeInnerClass() {
    def executionFailure: FailureClass = {
      FailureClass(GenericExecutionFailure(new RuntimeException("runtime", new IllegalArgumentException("causing"))), "task")
    }
  }

  it should "return the correct root class name when a stacktrace is available" in {
    val genericExecutionFailure = GenericExecutionFailure(new RuntimeException("runtime", new IllegalArgumentException("causing")))
    val failure = failureClass(genericExecutionFailure)
    failure.getRootClass mustBe thisClassName
    failure.getRootLine mustBe 18
  }

  it should "return the correct root class name when a stacktrace is not available" in {
    val genericExecutionFailure = GenericExecutionFailure(new Exception("Top exception")).
        copy(cause = Some(GenericExecutionFailure(new RuntimeException("runtime")).copy(stackTrace = None)))
    val failure = failureClass(genericExecutionFailure)
    failure.getRootClass mustBe "java.lang.RuntimeException"
    failure.getRootLine mustBe 0 // No line number available, since there is no stacktrace
  }

  it should "return the correct root class name when the exception was created in a nested class" in {
    val failure = SomeInnerClass().executionFailure
    failure.getRootClass mustBe thisClassName
    failure.getRootLine mustBe 13
  }

  private def failureClass(genericExecutionFailure: GenericExecutionFailure): FailureClass = {
    FailureClass(
      genericExecutionFailure,
      "taskID"
    )
  }

  it should "store the root cause" in {
    val failureClass = FailureClass(
      GenericExecutionFailure(new RuntimeException("runtime", new IllegalArgumentException("causing"))),
      "original message",
      "task",
      None
    )
    failureClass.originalMessage mustBe "runtime"
    failureClass.rootCause.className mustBe "java.lang.IllegalArgumentException"
  }

  it should "overwrite the message if the root failure has no cause" in {
    val failureClass = FailureClass(
      GenericExecutionFailure(new RuntimeException("runtime")),
      "original message",
      "task",
      None
    )
    failureClass.originalMessage mustBe "original message"
  }
}

