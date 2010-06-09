package de.fuberlin.wiwiss.silk.evaluation

import java.io.File
import xml.XML

/**
 * Reads the alignment format specified at http://alignapi.gforge.inria.fr/format.html.
 */
object AlignmentReader
{
     def read(file : File) : Traversable[Alignment] =
     {
         for(cell <- XML.loadFile(file) \ "Alignment" \ "map" \ "Cell") yield
         {
             new Alignment(sourceUri = cell \ "entity1" \ "@{http://www.w3.org/1999/02/22-rdf-syntax-ns#}resource" text,
                           targetUri = cell \ "entity2" \ "@{http://www.w3.org/1999/02/22-rdf-syntax-ns#}resource" text,
                           confidence = (cell \ "measure").text.toDouble)
         }
     }
}
