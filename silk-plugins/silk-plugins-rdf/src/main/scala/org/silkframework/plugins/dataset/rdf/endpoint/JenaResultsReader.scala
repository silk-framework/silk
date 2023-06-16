package org.silkframework.plugins.dataset.rdf.endpoint

import org.apache.jena.query.{QuerySolution, ResultSet}
import org.silkframework.dataset.rdf._
import org.silkframework.runtime.iterator.CloseableIterator

import scala.collection.immutable.SortedMap
import scala.jdk.CollectionConverters.IteratorHasAsScala

/**
  * Reads Jena ResultSets into Silk SPARQL results.
  */
object JenaResultsReader {

  /**
    * Iterator that reads the result set and closes it afterwards.
    */
  def read(resultSet: ResultSet): CloseableIterator[SortedMap[String, RdfNode]] = {
    val iterator =
      for (result: QuerySolution <- resultSet.asScala) yield {
        // Make sure that the reading has not been interrupted in the mean time
        if(Thread.currentThread().isInterrupted) {
          throw new InterruptedException()
        }
        toSilkBinding(result)
      }
    CloseableIterator(iterator, () => { resultSet.close() })
  }

  /**
    * Converts a Jena ARQ QuerySolution to a Silk binding
    */
  def toSilkBinding(querySolution: QuerySolution): SortedMap[String, RdfNode] = {
    val values =
      for (varName <- querySolution.varNames.asScala.toList;
           value <- Option(querySolution.get(varName))) yield {
        (varName, toSilkNode(value))
      }

    SortedMap(values: _*)
  }

  /**
    *  Converts a Jena RDFNode to a Silk Node.
    */
  def toSilkNode(node: org.apache.jena.rdf.model.RDFNode): RdfNode = node match {
    case r: org.apache.jena.rdf.model.Resource if !r.isAnon => Resource(r.getURI)
    case r: org.apache.jena.rdf.model.Resource => BlankNode(r.getId.getLabelString)
    case l: org.apache.jena.rdf.model.Literal =>
      val dataType = Option(l.getDatatypeURI).filterNot(_ == "http://www.w3.org/2001/XMLSchema#string")
      val lang = Option(l.getLanguage).filterNot(_.isEmpty)
      val lexicalValue = l.getString
      lang.map(LanguageLiteral(lexicalValue, _)).
        orElse(dataType.map(DataTypeLiteral(lexicalValue, _))).
        getOrElse(PlainLiteral(lexicalValue))
    case _ => throw new IllegalArgumentException("Unsupported Jena RDFNode type '" + node.getClass.getName + "' in Jena SPARQL results")
  }

}
