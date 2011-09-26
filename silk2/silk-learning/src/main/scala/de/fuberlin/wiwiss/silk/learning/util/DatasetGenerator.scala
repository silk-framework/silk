package de.fuberlin.wiwiss.silk.learning.util

import util.Random
import java.util.concurrent.atomic.AtomicInteger
import de.fuberlin.wiwiss.silk.instance.{Path, SparqlRestriction, InstanceSpecification, Instance}
import de.fuberlin.wiwiss.silk.util.SourceTargetPair
import java.io.{Writer, FileWriter}
import de.fuberlin.wiwiss.silk.evaluation.ReferenceLinks
import de.fuberlin.wiwiss.silk.output.Link

object DatasetGenerator {

  private val prefix = "http://silktest.org/"

  private val rdfs = "http://www.w3.org/2000/01/rdf-schema#"

  private val sourceWriter = new FileWriter("C:\\Users\\Robert\\.silk\\datasets\\testsource.nt")

  private val targetWriter = new FileWriter("C:\\Users\\Robert\\.silk\\datasets\\testtarget.nt")
  
  private var referenceLinks = ReferenceLinks()

  def main(args: Array[String]) {
    for(i <- 0 to 10000) {
      val id1 = GenerateId()
      val name1 = GenerateName()
      val date1 = GenerateDate()
      val id2 = GenerateId()
      val name2 = GenerateName()
      val date2 = GenerateDate()

      if(i % 1000 == 0) {
        //Movies with the same name can be from different years
        writePair(id1, name1, date1, id2, name1, date2)
        writePair(id2, name2, date1, id1, name2, date2)
      }
      else if(i % 1000 == 1) {
        //Movies with the same date can have different years
        writePair(id1, name1, date1, id2, name2, date1)
        writePair(id2, name2, date2, id1, name1, date2)
      }
      else {
        writePair(id1, name1, date1, id1, name1, date1)
        writePair(id2, name2, date2, id2, name2, date2)
      }
    }

    sourceWriter.close()
    targetWriter.close()
  }

  def writePair(id1: String, name1: String, date1: Int, id2: String, name2: String, date2: Int) {
    sourceWriter.write("<" + prefix + id1 + "S> <" + rdfs + "label> \"" + name1 + "\".\n")
    sourceWriter.write("<" + prefix + id1 + "S> <" + prefix + "date> \"" + date1 + "\".\n")
    targetWriter.write("<" + prefix + id2 + "T> <" + rdfs + "label> \"" + name2 + "\".\n")
    targetWriter.write("<" + prefix + id2 + "T> <" + prefix + "date> \"" + date2 + "\".\n")
  }

  private def addPositiveLink(name1: String, name2: String) {
    val link = new Link(prefix + name1 + "S", prefix + name2 + "T")
    referenceLinks = referenceLinks.copy(positive = referenceLinks.positive + link)
  }

  private def addNegativeLink(name1: String, name2: String) {
    val link = new Link(prefix + name1 + "S", prefix + name2 + "T")
    referenceLinks = referenceLinks.copy(negative = referenceLinks.negative + link)
  }

  private object GenerateId {
    private val count = new AtomicInteger(0)

    def apply() = {
      "M" + count.incrementAndGet()
    }
  }

  private object GenerateName {
    private val count = new AtomicInteger(0)

    def apply() = {
      "Movie" + count.incrementAndGet()
    }
  }

  private object GenerateDate {
    private val count = new AtomicInteger(0)

    def apply() = {
      1900 + count.getAndIncrement()
    }
  }
}