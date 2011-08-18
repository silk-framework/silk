//package de.fuberlin.wiwiss.silk.learning.util
//
//import io.Source
//import de.fuberlin.wiwiss.silk.evaluation.Alignment
//import de.fuberlin.wiwiss.silk.output.Link
//import de.fuberlin.wiwiss.silk.util.XMLUtils._
//import java.io.{File, Writer, FileWriter}
//import java.util.logging.Logger
//
//object Test {
//
//  val dir = new File("C:\\Users\\Robert\\Downloads\\btc\\data\\")
//
//  //val rdfType: String = "<http://xmlns.com/foaf/0.1/Person>"
//  val rdfType: String = "<http://www4.wiwiss.fu-berlin.de/drugbank/resource/drugbank/drugs>"
//
//  implicit val logger = Logger.getLogger(Test.getClass.getName)
//
//  //TODO by data source
//
//  def main(args: Array[String]) = {
//
//    //count()
//    collectTrainingset()
//
//    println("Done")
//  }
//
//  def count() {
//    val typeCounts = dir.listFiles.flatMap(countTypes)
//
//    val aggregatedTypeCounts = typeCounts.groupBy(_.typename).values.map(_.reduce((c1, c2) => TypeCount(c1.typename, c1.count + c2.count)))
//
//    for(c <- aggregatedTypeCounts.toSeq.sortBy(_.count)) {
//      println(c)
//    }
//  }
//
//  def collectTrainingset() {
//
//    val sameAs = dir.listFiles.flatMap(findSameAs)
//
//    val sameAsMap = sameAs.groupBy(_.target)
//
//    println("Collected " + sameAsMap.size + " sameAs links from persons")
//
//    val foundEntities = dir.listFiles.flatMap(file => findPairs(file, sameAsMap))
//
//    write(foundEntities.toTraversable)
//  }
//
//  def findSameAs(file: File) = {
//    val quads = readFile(file)
//    val entities = getTypedEntities(quads) //TODO remove entities without sameAs here
//
//    println("Collected " + entities.size + " persons in file " + file)
//
//    val sameAs = entities.flatMap(person => person.quads.filter(_.predicate == "<http://www.w3.org/2002/07/owl#sameAs>").map(quad => SameAs(person, quad.value)))
//
//    println("Collected " + sameAs.size + " sameAs links from persons in file " + file)
//
//    sameAs
//
//    //for(sameAs <- personsWithSameAs) println(sameAs.entity.subject + " " + sameAs.target)
//  }
//
//  def findPairs(file: File, sameAsMap: Map[String, Array[SameAs]]) =  {
//    val quads = readFile(file)
//    val foundEntities = getEntities(quads).flatMap(targetPerson => sameAsMap.get(targetPerson.subject).toArray.flatMap(_.map(sourcePerson => PersonPair(sourcePerson.entity, targetPerson))))
//
//    println("Collected " + foundEntities.size + " persons targeted by a sameAs link in file " + file)
//
//    foundEntities
//  }
//
//  def countTypes(file: File) = {
//    val typeCounts = readFile(file).filter(_.predicate == "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>")
//                                   .toArray.par
//                                   .groupBy(_.value)
//                                   .map(p => TypeCount(p._1, p._2.size))
//                                   .seq.toSeq.sortBy(_.count)
//
//    println("Counted types in " + file)
//
//    typeCounts
//  }
//
//  def write(pairs: Traversable[PersonPair]) {
//    val writer1 = new FileWriter("source.nt")
//    for(pair <- pairs) {
//      writeEntity(pair.e1, writer1)
//    }
//    writer1.close()
//
//    val writer2 = new FileWriter("target.nt")
//    for(pair <- pairs) {
//      writeEntity(pair.e2, writer2)
//    }
//    writer2.close()
//
//    Alignment(pairs.map(pair => new Link(pair.e1.subject.tail.init, pair.e2.subject.tail.init)).toSet).toXML.write(new File("alignment.xml"))
//  }
//
//  def writeEntity(entity: Entity, writer: Writer) {
//    for(quad <- entity.quads) {
//      writeQuad(quad, writer)
//    }
//  }
//
//  def writeQuad(quad: Quad, writer: Writer) {
//   writer.write(quad.toString)
//  }
//
//  def getTypedEntities(quads: Iterator[Quad]) = {
//    getEntities(quads).filter(_.typename == rdfType)
//  }
//
//  def getEntities(quads: Iterator[Quad]) = {
//    for((subject, quads) <- quads.toArray.par.groupBy(_.subject);
//        typeQuad <- quads.find(_.predicate == "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>")) yield {
//        Entity(subject, typeQuad.value, quads.seq)
//    }
//  }
//
//  def readFile(file: File): Iterator[Quad] = {
//    Source.fromFile(file).getLines().map(parse)
//  }
//
//  def parse(line: String) = {
//    val array = line.split(" ")
//    Quad(array(0), array(1), array.slice(2, array.length - 2).mkString(" "), null)
//  }
//
//  case class Quad(subject: String, predicate: String, value: String, provenance: String) {
//    override def toString = subject + " " + predicate + " " + value + " .\n"
//  }
//
//  case class Entity(subject: String, typename: String, quads: Traversable[Quad])
//
//  case class TypeCount(typename: String, count: Int) {
//    override def toString = typename + ": " + count
//  }
//
//  case class SameAs(entity: Entity, target: String)
//
//  case class PersonPair(e1: Entity, e2: Entity) {
//    override def toString = e1 + " " + e2 + "\n\n"
//  }
//}