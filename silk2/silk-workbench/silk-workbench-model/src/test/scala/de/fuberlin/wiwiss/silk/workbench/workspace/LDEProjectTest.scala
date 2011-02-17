package de.fuberlin.wiwiss.silk.workbench.workspace

import modules.linking.LinkingTask
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import de.fuberlin.wiwiss.silk.impl.DefaultImplementations
import de.fuberlin.wiwiss.silk.linkspec.{LinkFilter, LinkSpecification}

class LDEProjectTest extends FlatSpec with ShouldMatchers
{
 /*   DefaultImplementations.register()

    // Test LDEProject class

    val xmlPath =  "de/fuberlin/wiwiss/silk/linkspec/examples/linkedmdb_directors.xml"
    val proj = new LDEProject(xmlPath)
    val lt = proj.linkingModule.tasks.last

    // LinkingTask Tests

    "LDEProjectTest" should "- number of interlinks" in
    {
         proj.linkingModule.tasks.size should equal (1)
    }

    "LDEProjectTest" should "- interlink id value" in
    {
        proj.linkingModule.tasks.last.name should equal ("movies")
    }

    // test linkingModule update - new task
    "LDEProjectTest" should "- add new interlink" in
    {
      val ref = lt.linkSpec
      val task = new LinkingTask("actor",null,new LinkSpecification("actor",ref.linkType,ref.datasets,ref.condition,ref.filter,ref.outputs),null,null)
      proj.linkingModule.update(task)
      proj.linkingModule.tasks.last.name should equal ("actor")
    }

    // test linkingModule update - existing task
    "LDEProjectTest" should "- update interlink" in
    {
      val ref = lt.linkSpec
      val task = new LinkingTask("actor",null,new LinkSpecification("actor",ref.linkType,ref.datasets,ref.condition,new LinkFilter(),ref.outputs),null,null)
      proj.linkingModule.update(task)
      proj.linkingModule.tasks.filter(_.name == "actor").last.linkSpec.filter.threshold should equal(0.0)
    }

    // test linkingModule remove
    "LDEProjectTest" should "- remove interlink" in
    {
      proj.linkingModule.remove("movies")
      proj.linkingModule.tasks.filter(_.name == "movies").size should equal (0)
    }

    // test linkingModule remove - wrong id value (from source)
    "LDEProjectTest" should "- remove wrong interlink" in
    {
      proj.linkingModule.remove("linkedmdb")
      proj.linkingModule.tasks.size should equal (1)
    }

    // DataSource Tests

    "LDEProjectTest" should "- number of sources" in
    {
        proj.sourceModule.tasks.size should equal (2)
    }

    "LDEProjectTest" should "- source id value'" in
    {
        proj.sourceModule.tasks.last.name should equal ("linkedmdb")
    }

    "LDEProjectTest" should "- remove source" in
    {
        proj.sourceModule.remove("linkedmdb")
        proj.linkingModule.tasks.filter(_.name == "linkedmdb").size should equal (0)
    }*/
}