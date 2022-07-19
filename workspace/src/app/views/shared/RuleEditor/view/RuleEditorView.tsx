import { Divider, Grid, GridColumn, GridRow } from "@eccenca/gui-elements";
import { RuleEditorToolbar } from "./RuleEditorToolbar";
import { RuleEditorOperatorSidebar } from "./sidebar/RuleEditorOperatorSidebar";
import React from "react";
import { RuleEditorCanvas } from "./RuleEditorCanvas";
import { RuleEditorUiContext } from "../contexts/RuleEditorUiContext";
import { OnLoadParams } from "react-flow-renderer";

/** The main view of the rule editor, integrating toolbar, sidebar and main rule canvas. */
export const RuleEditorView = () => {
    const [modalShown, setModalShown] = React.useState(false);
    const [advancedParameterModeEnabled, setAdvancedParameterMode] = React.useState(false);
    const reactFlowWrapper = React.useRef<any>(null);
    const [reactFlowInstance, setReactFlowInstance] = React.useState<OnLoadParams | undefined>(undefined);

    return (
        <RuleEditorUiContext.Provider
            value={{
                modalShown,
                setModalShown,
                advancedParameterModeEnabled,
                setAdvancedParameterMode,
                reactFlowWrapper,
                reactFlowInstance,
                setReactFlowInstance,
            }}
        >
            <Grid verticalStretchable={true} useAbsoluteSpace={true} style={{ backgroundColor: "white" }}>
                <GridRow style={{ backgroundColor: "white" }}>
                    <GridColumn full>
                        <RuleEditorToolbar />
                        <Divider addSpacing="medium" />
                    </GridColumn>
                </GridRow>
                <GridRow verticalStretched={true} style={{ backgroundColor: "white" }}>
                    <GridColumn small>
                        <RuleEditorOperatorSidebar />
                    </GridColumn>
                    <RuleEditorCanvas />
                </GridRow>
            </Grid>
        </RuleEditorUiContext.Provider>
    );
};
