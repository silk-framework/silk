package de.fuberlin.wiwiss.silk.datasource

import de.fuberlin.wiwiss.silk.Instance
import de.fuberlin.wiwiss.silk.util.FileUtils._
import java.io.{File, ObjectOutputStream, FileOutputStream}

class ClusterWriter(dir : File)
{
    private val clusterSize = 1000

    def write(instances : Traversable[Instance])
    {
        dir.deleteRecursive()
        dir.mkdirs()

        var curCluster = List[Instance]()
        var curClusterSize = 0
        var curClusterIndex = 0

        for(instance <- instances)
        {
            curCluster ::= instance
            curClusterSize += 1

            if(curClusterSize == clusterSize)
            {
                writeCluster(curCluster, curClusterIndex)

                curCluster = List[Instance]()
                curClusterSize = 0
                curClusterIndex += 1
            }
        }
    }

    private def writeCluster(instances : List[Instance], index : Int)
    {
        val stream = new ObjectOutputStream(new FileOutputStream(dir + "/cluster" + index))

        try
        {
            stream.writeInt(clusterSize)
            for(instance <- instances)
            {
                stream.writeObject(instance)
            }
        }
        finally
        {
            stream.close()
        }

        println("Written cluster " + index)
    }
}
