import React from "react";
import { IconButton, Spacing, Toolbar, ToolbarSection } from "gui-elements";
import { RuleEditorModelContext } from "../contexts/RuleEditorModelContext";

/** Toolbar of the rule editor. Contains global editor actions like save, redo/undo etc. */
export const RuleEditorToolbar = () => {
    const modelContext = React.useContext(RuleEditorModelContext);

    return (
        <Toolbar data-test-id={"workflow-editor-header"} noWrap>
            <ToolbarSection>
                <IconButton
                    data-test-id={"rule-editor-undo-btn"}
                    disabled={!modelContext.canUndo}
                    name="operation-undo"
                    text="Undo"
                    onClick={modelContext.undo}
                />
                <IconButton
                    data-test-id={"rule-editor-redo-btn"}
                    disabled={!modelContext.canRedo}
                    name="operation-redo"
                    text="Redo"
                    onClick={modelContext.redo}
                />
                <Spacing vertical hasDivider />
                <IconButton
                    data-test-id={"rule-editor-auto-layout-btn"}
                    disabled={modelContext.elements.length === 0}
                    name="operation-auto-graph-layout"
                    text="Auto-layout"
                    onClick={modelContext.executeModelEditOperation.autoLayout}
                />
            </ToolbarSection>
        </Toolbar>
    );
};
