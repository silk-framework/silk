package org.silkframework.dataset

import org.silkframework.runtime.activity.UserContext
import CloseableDataset.using
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class CloseableDatasetTest extends AnyFlatSpec with Matchers {

  behavior of "CloseableDataset"

  it must "autoclose dataset and don't swallow exceptions" in {
    implicit val userContext = UserContext.Empty
    val thrown = the[TestException] thrownBy {
      using(new TestDataset) { dataset =>
        dataset.doSomething()
      }
    }
    thrown.getSuppressed mustBe Array(CloseException)
  }

  case class TestDataset() extends CloseableDataset {

    def doSomething(): Unit = {
      throw new TestException
    }

    override def close()(implicit userContext: UserContext): Unit = {
      throw CloseException
    }
  }

  class TestException extends Exception

  object CloseException extends Exception

}
