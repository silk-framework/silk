package de.fuberlin.wiwiss.silk.workbench.workspace

import de.fuberlin.wiwiss.silk.linkspec.LinkSpecification
import de.fuberlin.wiwiss.silk.workbench.project.Cache
import de.fuberlin.wiwiss.silk.evaluation.Alignment

case class LinkingTask(name : String,
                       linkSpec : LinkSpecification,
                       alignment : Alignment,
                       cache : Cache) extends Module
