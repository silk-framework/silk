package de.fuberlin.wiwiss.silk.hadoop

import java.util.logging.Logger
import org.apache.hadoop.conf.Configured
import org.apache.hadoop.util.{ToolRunner, Tool}

object Silk
{
    private val logger = Logger.getLogger(Silk.getClass.getName)

    def main(args : Array[String])
    {
        val res = ToolRunner.run(new Silk(), args)
        System.exit(res)
    }
}

class Silk extends Configured with Tool
{
    def run(args : Array[String]) : Int =
    {
        args match
        {
            case Array("load", configFile, outputDir)           => new Load(configFile, outputDir, None          , getConf())()
            case Array("load", configFile, outputDir, linkSpec) => new Load(configFile, outputDir, Some(linkSpec), getConf())()
            case Array("match", inputDir, outputDir)            => new Match(inputDir , outputDir, None          , getConf())()
            case Array("match", inputDir, outputDir, linkSpec)  => new Match(inputDir , outputDir, Some(linkSpec), getConf())()
            case _ => printUsage()
        }

        0
    }

    private def printUsage()
    {
        println("usage:")
        println("  load configFile ouputDir [linkSpec]")
        println("  match inputDir ouputDir [linkSpec]")
    }
}
