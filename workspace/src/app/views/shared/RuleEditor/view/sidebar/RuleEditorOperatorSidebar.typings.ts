import type React from "react";
import {
    IRuleOperator,
    RuleEditorPatchableNodeProjection,
    RuleEditorSidebarDragPayload,
} from "../../RuleEditor.typings";
import { RuleEditorNodeParameterValue } from "../../model/RuleEditorModel.typings";

/** The operator data that is used for rendering a rule operator in the sidebar. */
export interface SidebarRuleOperatorBase extends Omit<IRuleOperator, "portSpecification" | "parameterSpecification"> {}

/** A rule operator plugin version that has initial parameter values that are different from the plugin specification. */
export interface IPreConfiguredRuleOperator extends SidebarRuleOperatorBase {
    /** New initial values for this pre-configured operator. */
    parameterOverwrites: {
        [key: string]: RuleEditorNodeParameterValue;
    };
    /** Optional initial metadata projection for the created node. */
    nodeMetaDataOverwrites?: RuleEditorPatchableNodeProjection;
    /** If false, this pre-configured entry cannot be dragged onto the canvas. Defaults to true. */
    draggable?: boolean;
    /** Optional sidebar actions rendered for this pre-configured entry. */
    actions?: React.JSX.Element | React.JSX.Element[];
    /** Optional custom drag payload for special create-on-drop sidebar entries. */
    dragData?: RuleEditorSidebarDragPayload;
}
