package org.silkframework.dataset.rdf

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.runtime.activity.UserContext
import org.silkframework.util.{DynamicContent, MockServerTestTrait, ServedContent}

import java.net.URLEncoder

class GraphStoreTraitTest extends AnyFlatSpec with Matchers with MockServerTestTrait {

  behavior of "GraphStoreTrait"

  private implicit val userContext: UserContext = UserContext.Empty
  private val graph = "http://example.org/graph"

  private class TestGraphStore(port: Int) extends GraphStoreTrait {
    override def graphStoreEndpoint(graph: String): String =
      s"http://localhost:$port/graphstore?graph=" + URLEncoder.encode(graph, "UTF-8")
  }

  it should "fail deleteGraph with an error when the server keeps answering with server errors instead of returning silently" in {
    var requests = 0
    withAdditionalServer(Seq(DynamicContent("/graphstore", _ => {
      requests += 1
      ServedContent(statusCode = INTERNAL_ERROR)
    }))) { port =>
      val error = intercept[RuntimeException] {
        new TestGraphStore(port).deleteGraph(graph)
      }
      error.getMessage should include("500")
      requests shouldBe 2 // the server error is retried once
    }
  }

  it should "delete a graph on the second attempt after a server error" in {
    var requests = 0
    withAdditionalServer(Seq(DynamicContent("/graphstore", _ => {
      requests += 1
      if (requests == 1) ServedContent(statusCode = INTERNAL_ERROR) else ServedContent(statusCode = NO_CONTENT)
    }))) { port =>
      noException should be thrownBy new TestGraphStore(port).deleteGraph(graph)
      requests shouldBe 2
    }
  }

  it should "ignore a missing graph on deleteGraph if requested" in {
    withAdditionalServer(Seq(DynamicContent("/graphstore", _ => ServedContent(statusCode = NOT_FOUND)))) { port =>
      noException should be thrownBy new TestGraphStore(port).deleteGraph(graph, ignoreIfNotExists = true)
    }
  }
}
