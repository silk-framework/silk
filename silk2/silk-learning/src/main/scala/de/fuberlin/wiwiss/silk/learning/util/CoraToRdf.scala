package de.fuberlin.wiwiss.silk.learning.util

import xml.{NodeSeq, Node, XML}
import java.io.{FileWriter, File}

object CoraToRdf {
  private val file = "C:\\Users\\Robert\\Downloads\\cora-all-id.xml"

  private val outputFile = "C:\\Users\\Robert\\Downloads\\cora-all-id.nt"

  private val referenceFile = "C:\\Users\\Robert\\Downloads\\cora-all-ref.nt"

  private val prefix = "http://test.org/"

  private val Author = "<" + prefix + "author>"
  private val Title = "<" + prefix + "title>"
  private val Venue = "<" + prefix + "venue>"
  private val Date = "<" + prefix + "date>"

  private var entities = List[Entity]()

  private var count = 0

  def main(args: Array[String]) {
    val xml = XML.loadFile(new File(file))

    val triples = (xml \ "publication").flatMap(publication)

    val fos = new FileWriter(outputFile)
    triples.map(fos.write)
    fos.close()

    val fos2 = new FileWriter(referenceFile)

//    for(group <- entities.groupBy(_.id).values;
//        source :: targets <- (group ++ group).sliding(group.size).take(group.size);
//        target <- targets) {
//      fos2.write(source.uri + " <http://www.w3.org/2002/07/owl#sameAs> " + target.uri + ".\n")
//    }

    for(source :: targets <- entities.groupBy(_.id).values;
        target <- targets) {
      fos2.write(source.uri + " <http://www.w3.org/2002/07/owl#sameAs> " + target.uri + ".\n")
    }

    fos2.close()
  }

  private def publication(node: Node) = {
    val subject = createSubject(node \ "@id" text)

    convert(subject, Author, node \ "author") ::
    convert(subject, Title, node \ "title") ::
    convert(subject, Venue, node \ "venue" \ "venue" \ "name") ::
    convert(subject, Date, node \ "venue" \ "venue" \ "date") :: Nil
  }.flatten

  private def createSubject(id: String) = {
    val entity = Entity(id, "<"+ prefix + id + "_" + count + ">")
    count += 1
    entities ::= entity
    entity
  }

  private def convert(subject: Entity, property: String, nodes: NodeSeq) = for(node <- nodes) yield subject.uri + " " + property + " \"" + encode(node.text) + "\".\n"

  private def encode(str: String) = {
    str.replace("\"", "\\\"")
  }

  private case class Entity(id: String, uri: String)
}