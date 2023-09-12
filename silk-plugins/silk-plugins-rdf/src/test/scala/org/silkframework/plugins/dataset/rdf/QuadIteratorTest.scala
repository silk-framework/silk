package org.silkframework.plugins.dataset.rdf

import java.io.StringReader
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.impl.StatementImpl

import org.silkframework.dataset.rdf.{IteratorFormatter, QuadIterator}
import org.silkframework.plugins.dataset.rdf.datasets.RdfFileDataset
import org.silkframework.plugins.dataset.rdf.formatters.NTriplesQuadFormatter
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource.{ClasspathResourceLoader, ReadOnlyResourceManager}
import org.silkframework.util.MockitoSugar
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class QuadIteratorTest extends AnyFlatSpec with Matchers with MockitoSugar {

  implicit val uc: UserContext = UserContext.Empty
  private lazy val resources = ReadOnlyResourceManager(ClasspathResourceLoader(getClass.getPackage.getName.replace('.', '/')))
  private val source = RdfFileDataset(resources.get("target.nt"), "N-Triples")

  it should "should produce isomorphic graphs when serializing the origin graph with QuadIterator" in {
    val quadIterator = source.sparqlEndpoint.construct("CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o }")
    val serialized = serialize(quadIterator)
    var model = ModelFactory.createDefaultModel()
    model.read(new StringReader(serialized), null, "N-Quads")

    val oldModel = source.sparqlEndpoint.constructModel("CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o }")
    // the serialized graph (via QuadIterator) should be isomorphic to the origin graph
    model.isEmpty shouldBe false
    oldModel.isEmpty shouldBe false
    if(!oldModel.isIsomorphicWith(model)) {
      fail(model.toString + " is not isomorphic with " + oldModel)
    }

    // adding some edge cases
    oldModel.add(      new StatementImpl(
      oldModel.createResource("http://example.org/test/test/1"),
      oldModel.createProperty("http://example.org/prop/1"),
      oldModel.createLiteral("shdfvjs se\\jdsö\\\\\\nsjhf\"df")
    ))

    // now we parse the serialized graph back to a QuadIterator
    val newQuadIterator = JenaModelTripleIterator(oldModel)

    val newSerialization = serialize(newQuadIterator) +
      "<http://example.org/test/test/1> <http://example.org/prop/1> \"shdfvjs se\\\\jdsö\\\\\\\\\\\\nsjhf\\\"df\"^^<http://www.w3.org/2001/XMLSchema#string> ."
    model = ModelFactory.createDefaultModel()
    model.read(new StringReader(newSerialization), null, "N-Quads")

    // the twice serialized graph should be isomorphic to the origin graph
    if(!oldModel.isIsomorphicWith(model)) {
      fail(model.toString + " is not isomorphic with " + oldModel)
    }
  }

  private def serialize(quadIterator: QuadIterator): String = {
    IteratorFormatter.serialize(quadIterator, new NTriplesQuadFormatter())
  }
}
