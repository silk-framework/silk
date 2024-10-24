import { Divider, Grid, GridColumn, GridRow } from "@eccenca/gui-elements";
import { RuleEditorToolbar } from "./RuleEditorToolbar";
import { RuleEditorOperatorSidebar } from "./sidebar/RuleEditorOperatorSidebar";
import React from "react";
import { RuleEditorCanvas } from "./RuleEditorCanvas";
import { RuleEditorUiContext } from "../contexts/RuleEditorUiContext";
import { OnLoadParams } from "react-flow-renderer";

interface RuleEditorViewProps {
    /** When enabled only the rule is shown without side- and toolbar and any other means to edit the rule. */
    showRuleOnly?: boolean;
    /** When enabled the mini map is not displayed. */
    hideMinimap?: boolean;
    /** Defines minimun and maximum of the available zoom levels */
    zoomRange?: [number, number];
    /** In the permanent read-only mode the sidebar will be removed.*/
    readOnlyMode: boolean;
}

/** The main view of the rule editor, integrating toolbar, sidebar and main rule canvas. */
export const RuleEditorView = ({ showRuleOnly, hideMinimap, zoomRange, readOnlyMode }: RuleEditorViewProps) => {
    const [modalShown, setModalShown] = React.useState(false);
    const [advancedParameterModeEnabled, setAdvancedParameterMode] = React.useState(false);
    const [currentRuleNodeDescription, setCurrentRuleNodeDescription] = React.useState<string | undefined>("");
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
                currentRuleNodeDescription,
                setCurrentRuleNodeDescription,
                showRuleOnly,
                hideMinimap,
                zoomRange,
            }}
        >
            <Grid verticalStretchable={true} useAbsoluteSpace={true} style={{ backgroundColor: "white" }}>
                {!showRuleOnly ? (
                    <GridRow style={{ backgroundColor: "white" }}>
                        <GridColumn>
                            <RuleEditorToolbar />
                            <Divider addSpacing="medium" />
                        </GridColumn>
                    </GridRow>
                ) : null}
                <GridRow verticalStretched={true} style={{ backgroundColor: "white" }}>
                    {!showRuleOnly && !readOnlyMode ? (
                        <GridColumn small>
                            <RuleEditorOperatorSidebar />
                        </GridColumn>
                    ) : null}
                    <RuleEditorCanvas />
                </GridRow>
            </Grid>
        </RuleEditorUiContext.Provider>
    );
};
