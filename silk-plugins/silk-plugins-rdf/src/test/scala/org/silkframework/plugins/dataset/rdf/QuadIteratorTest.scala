package org.silkframework.plugins.dataset.rdf

import java.io.StringReader

import org.apache.jena.rdf.model.ModelFactory
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FlatSpec, Matchers}
import org.silkframework.plugins.dataset.rdf.datasets.RdfFileDataset
import org.silkframework.plugins.dataset.rdf.endpoint.JenaEndpoint
import org.silkframework.plugins.dataset.rdf.formatters.NTriplesQuadFormatter
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource.{ClasspathResourceLoader, ReadOnlyResourceManager}

class QuadIteratorTest extends FlatSpec with Matchers with MockitoSugar {

  implicit val uc: UserContext = UserContext.Empty
  private lazy val resources = ReadOnlyResourceManager(ClasspathResourceLoader(getClass.getPackage.getName.replace('.', '/')))
  private val source = RdfFileDataset(resources.get("target.nt"), "N-Triples")

  it should "should produce isomorphic graphs when serializing the origin graph with QuadIterator" in {
    val quadIterator = source.sparqlEndpoint.construct("CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o }")
    val serialized = quadIterator.serialize(asQuads = false)
    var model = ModelFactory.createDefaultModel()
    model.read(new StringReader(serialized), null, "N-Quads")

    val oldModel = source.sparqlEndpoint.constructModel("CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o }")
    // the serialized graph (via QuadIterator) should be isomorphic to the origin graph
    model.isEmpty shouldBe false
    oldModel.isEmpty shouldBe false
    oldModel.isIsomorphicWith(model) shouldBe true

    // now we parse the serialized graph back to a QuadIterator
    val iter = model.listStatements()
    val newQuadIterator = new QuadIteratorImpl(
      iter.hasNext,
      () => JenaEndpoint.jenaStatementToQuad(iter.next()),
      () => iter.close(),
      new NTriplesQuadFormatter
    )

    val newSerialization = newQuadIterator.serialize(asQuads = false)
    model = ModelFactory.createDefaultModel()
    model.read(new StringReader(newSerialization), null, "N-Quads")

    // the twice serialized graph should be isomorphic to the origin graph
    oldModel.isIsomorphicWith(model) shouldBe true
  }

}
