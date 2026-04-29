package org.silkframework.plugins.dataset.rdf.endpoint

import org.apache.jena.rdf.model.ModelFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource.Resource
import org.silkframework.util.ConfigTestTrait

class JenaModelEndpointTest extends AnyFlatSpec with Matchers {

  private implicit val userContext: UserContext = UserContext.Empty

  behavior of "JenaModelEndpoint"

  it should "not throw when data written is within the memory limit" in {
    ConfigTestTrait.withConfig(Resource.maxInMemorySizeParameterName -> Some("100b")) {
      val endpoint = new JenaModelEndpoint(ModelFactory.createDefaultModel())
      // Two triples at 9+8+9 bytes each = 52 estimated bytes, under the 100b limit
      endpoint.update("INSERT DATA { <http://s1> <http://p> <http://o1> }")
      endpoint.update("INSERT DATA { <http://s2> <http://p> <http://o2> }")
    }
  }

  it should "throw when data written exceeds the memory limit" in {
    ConfigTestTrait.withConfig(Resource.maxInMemorySizeParameterName -> Some("50b")) {
      val endpoint = new JenaModelEndpoint(ModelFactory.createDefaultModel())
      // First triple: 26 estimated bytes, within the 50b limit
      endpoint.update("INSERT DATA { <http://s1> <http://p> <http://o1> }")
      // Second triple pushes the total to 52 bytes, exceeding the limit
      an[RuntimeException] should be thrownBy {
        endpoint.update("INSERT DATA { <http://s2> <http://p> <http://o2> }")
      }
    }
  }

  it should "throw for a short generative update that produces more data than the query string" in {
    ConfigTestTrait.withConfig(Resource.maxInMemorySizeParameterName -> Some("50b")) {
      val endpoint = new JenaModelEndpoint(ModelFactory.createDefaultModel())
      // Write one triple (26 estimated bytes), within the 50b limit
      endpoint.update("INSERT DATA { <http://s1> <http://p> <http://o1> }")
      // A 47-char WHERE-clause query that generates a new triple:
      // <http://s1> <http://new-prop> <http://o1> → 9+15+9 = 33 more bytes, total 59 > 50b
      an[RuntimeException] should be thrownBy {
        endpoint.update("INSERT { ?s <http://new-prop> ?o } WHERE { ?s ?p ?o }")
      }
    }
  }
}
