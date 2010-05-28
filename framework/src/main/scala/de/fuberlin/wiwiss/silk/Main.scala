package de.fuberlin.wiwiss.silk

import java.io.File
import linkspec.ConfigLoader

object Main
{
    def main(args : Array[String])
    {
        val configFile = new File("C:/Users/Anja/silk/linkedmdb.xml");
        ConfigLoader.load(configFile)
        /*
        val input = new CharSequenceReader("""?artist/dbpedia:director[rdf:type = dbpedia:Album]/rdfs:label[@lang = 'en']""")
        val parser = new PathParser()

        val parseResult = parser.parseAll(parser.path, input)
        
        println(parseResult)

        if(parseResult.successful)
        {
            println("SPARQL: " + SparqlBuilder.build(parseResult.get))
        }
        */
    }
}
