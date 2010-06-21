package de.fuberlin.wiwiss.silk.evaluation

import java.io.File
import xml.XML
import io.Source

/**
 * Reads the alignment format specified at http://alignapi.gforge.inria.fr/format.html.
 */
object AlignmentReader
{
    def read(file : File) : Traversable[Alignment] =
    {
        file.getName.split("\\.").last match
        {
            case "n3debug" => readN3Debug(file)
            case "xml" => readAlignment(file)
            case _ => Traversable()
        }

    }

     def readAlignment(file : File) : Traversable[Alignment] =
     {
         for(cell <- XML.loadFile(file) \ "Alignment" \ "map" \ "Cell") yield
         {
             new Alignment(sourceUri = cell \ "entity1" \ "@{http://www.w3.org/1999/02/22-rdf-syntax-ns#}resource" text,
                           targetUri = cell \ "entity2" \ "@{http://www.w3.org/1999/02/22-rdf-syntax-ns#}resource" text,
                           confidence = (cell \ "measure").text.toDouble)
         }
     }

    private val N3DebugRegex = """\[(\d*\.\d*)\]:\s*<([^>]*)>\s*<[^>]*>\s*<([^>]*)>\s*\.\s*""".r

    def readN3Debug(file : File) : Traversable[Alignment] =
     {
         val source = Source.fromFile(file)
         for(N3DebugRegex(confidence, sourceUri, targetUri) <- source.getLines().toList) yield
         {
             new Alignment(sourceUri, targetUri, confidence.toDouble)
         }
     }
}
