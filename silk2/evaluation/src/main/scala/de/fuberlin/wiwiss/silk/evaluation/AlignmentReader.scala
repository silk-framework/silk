package de.fuberlin.wiwiss.silk.evaluation

import java.io.File
import xml.XML
import io.Source
import de.fuberlin.wiwiss.silk.output.Link

/**
 * Reads the alignment format specified at http://alignapi.gforge.inria.fr/format.html.
 */
object AlignmentReader
{
    def read(file : File) : Traversable[Link] =
    {
        file.getName.split("\\.").last match
        {
            case "n3debug" => readN3Debug(file)
            case "xml" => readAlignment(file)
            case _ => Traversable()
        }

    }

     def readAlignment(file : File) : Traversable[Link] =
     {
         for(cell <- XML.loadFile(file) \ "Alignment" \ "map" \ "Cell") yield
         {
             new Link(sourceUri = cell \ "entity1" \ "@{http://www.w3.org/1999/02/22-rdf-syntax-ns#}resource" text,
                           targetUri = cell \ "entity2" \ "@{http://www.w3.org/1999/02/22-rdf-syntax-ns#}resource" text,
                           confidence = (cell \ "measure").text.toDouble)
         }
     }

    private val N3DebugRegex = """\[(\d*\.\d*)\]:\s*<([^>]*)>\s*<[^>]*>\s*<([^>]*)>\s*\.\s*""".r

    def readN3Debug(file : File) : Traversable[Link] =
     {
         val source = Source.fromFile(file)
         for(N3DebugRegex(confidence, sourceUri, targetUri) <- source.getLines().toList) yield
         {
             new Link(sourceUri, targetUri, confidence.toDouble)
         }
     }
}
