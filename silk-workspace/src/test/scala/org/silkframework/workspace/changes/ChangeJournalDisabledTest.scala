package org.silkframework.workspace.changes

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.rule.{DirectMapping, MappingRules, MappingTarget, RootMappingRule, TransformSpec}
import org.silkframework.runtime.activity.TestUserContextTrait
import org.silkframework.runtime.templating.SimpleSubstitutionTemplateEngine
import org.silkframework.runtime.validation.NotFoundException
import org.silkframework.util.ConfigTestTrait
import org.silkframework.workspace.TestWorkspaceProviderTestTrait

class ChangeJournalDisabledTest extends AnyFlatSpec with Matchers with TestWorkspaceProviderTestTrait with TestUserContextTrait with ConfigTestTrait {

  behavior of "ChangeJournal with the store disabled"

  override def workspaceProviderId: String = "inMemoryWorkspaceProvider"

  // The jinja engine is not on this module's classpath; the simple engine substitutes '{{scope.name}}' references.
  override def propertyMap: Map[String, Option[String]] = Map(
    "workspace.changes.plugin" -> Some("emptyChangeJournal"),
    "config.variables.engine" -> Some(SimpleSubstitutionTemplateEngine.id))

  it should "record nothing and have nothing to revert" in {
    val project = retrieveOrCreateProject("journalDisabled")
    val rule = DirectMapping(id = "name", sourcePath = UntypedPath("name"), mappingTarget = MappingTarget("http://example.org/name"))
    project.addTask[TransformSpec]("transform", TransformSpec(mappingRule = RootMappingRule(MappingRules(propertyRules = Seq(rule)))))

    project.changeJournal.all shouldBe empty
    a[NotFoundException] should be thrownBy project.changeJournal.revert(1)
  }
}
