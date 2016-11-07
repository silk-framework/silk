package org.silkframework.workspace.activity.workflow

case class WorkflowLayoutConfig(// Horizontal distance in pixels between elements
                                horizontalPadding: Int,
                                // Vertical distance in pixels between elements
                                verticalPadding: Int,
                                elementHeight: Int,
                                elementWidth: Int,
                                // Horizontal offset where the upper-most elements should be placed
                                offsetX: Int,
                                // Vertical offset where the upper-most elements should be placed
                                offsetY: Int
                               )