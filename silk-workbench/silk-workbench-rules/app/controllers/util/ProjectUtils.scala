package controllers.util

import java.io.StringWriter

import com.hp.hpl.jena.rdf.model.{ModelFactory, Model}
import controllers.transform.TransformTaskApi._
import org.silkframework.dataset.{DataSink, Dataset, DataSource}
import org.silkframework.plugins.dataset.rdf.endpoint.JenaModelEndpoint
import org.silkframework.plugins.dataset.rdf.{SparqlParams, SparqlSink}
import org.silkframework.runtime.resource.{InMemoryResourceManager, ResourceManager}
import org.silkframework.runtime.serialization.Serialization
import org.silkframework.workspace.{User, Task, Project}
import play.api.mvc.Result

import scala.reflect.ClassTag
import scala.xml.NodeSeq

/**
 * Created by andreas on 12/10/15.
 */
object ProjectUtils {
  def getProjectAndTask[T: ClassTag](projectName: String, taskName: String): (Project, Task[T]) = {
    val project = User().workspace.project(projectName)
    val task = project.task[T](taskName)
    (project, task)
  }

  def nTriplesModelResult(model: Model): Result = {
    val writer = new StringWriter()
    model.write(writer, "N-Triples")
    Ok(writer.toString).as("application/n-triples")
  }

  def createDataSource(xmlRoot: NodeSeq)
                      (implicit resourceLoader: ResourceManager): DataSource = {
    val dataSource = xmlRoot \ "DataSources"
    if(dataSource.isEmpty) {
      throw new IllegalArgumentException("No data sources specified")
    }
    val dataset = Serialization.fromXml[Dataset]((dataSource \ "_").head)
    dataset.source
  }

  def createDataSink(xmlRoot: NodeSeq): (Model, DataSink) = {
    val dataSink = xmlRoot \ "dataSink"
    if (dataSink.isEmpty) {
      val model = ModelFactory.createDefaultModel()
      val inmemoryModelSink = new SparqlSink(SparqlParams(parallel = false), new JenaModelEndpoint(model))
      (model, inmemoryModelSink)
    } else {
      //      val dataSink = Serialization.fromXml[DataSink]()
      ???
    }
  }

  def createInmemoryResourceManagerForResource(inputResource: NodeSeq): ResourceManager = {
    val resourceId = inputResource \ "@name"
    val resourceManager = InMemoryResourceManager()
    resourceManager.
        get(resourceId.text).
        write(inputResource.text)
    resourceManager
  }
}
