package controllers.transform

import java.time.Instant

import helper.IntegrationTestTrait
import org.scalatestplus.play.PlaySpec
import org.silkframework.config.MetaData

class LinkingTaskApiTest extends PlaySpec with IntegrationTestTrait {

  private val project = "project"
  private val task = "linking"
  private val sourceDataset = "sourceDs"
  private val targetDataset = "targetDs"
  private val outputDataset = "outputDs"

  private val metaData =
    MetaData(
      label = "my linking task",
      description = Some("some comment"),
      modified = Some(Instant.now)
    )

  override def workspaceProvider = "inMemory"

  protected override def routes = Some("test.Routes")

  "Setup project" in {
    createProject(project)
    addProjectPrefixes(project)
    createVariableDataset(project, sourceDataset)
    createVariableDataset(project, targetDataset)
    createVariableDataset(project, outputDataset)
  }

  "Add a linking task" in {
    createLinkingTask(project, task, sourceDataset, targetDataset, outputDataset)
  }

  "Update meta data" in {
    updateMetaData(project, task, metaData)
  }

  "Update linkage rule" in {
    setLinkingRule(project, task,
      <LinkageRule linkType="&lt;http://www.w3.org/2002/07/owl#sameAs&gt;">
        <Aggregate id="combineSimilarities" required="false" weight="1" type="min">
          <Compare id="compareTitles" required="false" weight="1" metric="levenshteinDistance" threshold="0.0" indexing="true">
            <TransformInput id="toLowerCase1" function="lowerCase">
              <Input id="movieTitle1" path="&lt;http://xmlns.com/foaf/0.1/name&gt;"/>
            </TransformInput>
            <TransformInput id="toLowerCase2" function="lowerCase">
              <Input id="movieTitle2" path="&lt;http://www.w3.org/2000/01/rdf-schema#label&gt;"/>
            </TransformInput>
          </Compare>
        </Aggregate>
      </LinkageRule>
    )
  }

  "Check meta data" in {
    // Compare ignoring modified date
    getMetaData(project, task).copy(modified = metaData.modified) mustBe metaData
  }

}
