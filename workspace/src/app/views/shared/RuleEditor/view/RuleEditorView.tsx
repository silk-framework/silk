import { Divider, Grid, GridColumn, GridRow } from "gui-elements";
import { RuleEditorToolbar } from "./RuleEditorToolbar";
import { RuleEditorOperatorSidebar } from "./sidebar/RuleEditorOperatorSidebar";
import React from "react";
import { RuleEditorCanvas } from "./RuleEditorCanvas";

/** The main view of the rule editor, integrating toolbar, sidebar and main rule canvas. */
export const RuleEditorView = () => {
    return (
        <Grid verticalStretchable={true}>
            <GridRow>
                <GridColumn full>
                    <RuleEditorToolbar />
                    <Divider addSpacing="medium" />
                </GridColumn>
            </GridRow>
            <GridRow verticalStretched={true}>
                <GridColumn small>
                    <RuleEditorOperatorSidebar />
                </GridColumn>
                <RuleEditorCanvas />
            </GridRow>
        </Grid>
    );
};
