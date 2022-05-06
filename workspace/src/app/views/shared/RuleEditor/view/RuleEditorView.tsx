import { Divider, Grid, GridColumn, GridRow } from "@eccenca/gui-elements";
import { RuleEditorToolbar } from "./RuleEditorToolbar";
import { RuleEditorOperatorSidebar } from "./sidebar/RuleEditorOperatorSidebar";
import React from "react";
import { RuleEditorCanvas } from "./RuleEditorCanvas";
import { RuleEditorUiContext } from "../contexts/RuleEditorUiContext";

/** The main view of the rule editor, integrating toolbar, sidebar and main rule canvas. */
export const RuleEditorView = () => {
    const [modalShown, setModalShown] = React.useState(false);

    return (
        <RuleEditorUiContext.Provider
            value={{
                modalShown,
                setModalShown,
            }}
        >
            <Grid verticalStretchable={true} useAbsoluteSpace={true} style={{ backgroundColor: "white" }}>
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
        </RuleEditorUiContext.Provider>
    );
};
