package de.fuberlin.wiwiss.silk.workspace.modules.transform

import de.fuberlin.wiwiss.silk.workspace.modules.Module

/**
 * The transform module, which encapsulates all transform tasks.
 */
trait TransformModule extends Module[TransformConfig, TransformTask]