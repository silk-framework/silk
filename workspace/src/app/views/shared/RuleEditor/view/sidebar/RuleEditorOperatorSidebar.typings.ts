import type React from "react";
import { ValidIconName } from "@eccenca/gui-elements/src/components/atoms/Icon/canonicalIconNames";
import {
    IRuleOperator,
    RuleEditorPatchableNodeProjection,
    RuleEditorSidebarDragPayload,
} from "../../RuleEditor.typings";
import { RuleEditorNodeParameterValue } from "../../model/RuleEditorModel.typings";

/** Optional small status icon rendered next to a sidebar operator label. */
export interface SidebarRuleOperatorStatusIndicator {
    /** Icon shown next to the operator label. */
    icon: ValidIconName;
    /** Intent color used for the icon. */
    intent?: "warning" | "danger" | "success" | "info" | "accent";
    /** Tooltip shown on hover. */
    tooltipText: string;
}

/** The operator data that is used for rendering a rule operator in the sidebar. */
export interface SidebarRuleOperatorBase extends Omit<IRuleOperator, "portSpecification" | "parameterSpecification"> {
    /** Optional state indicator shown next to the label, e.g. for warnings. */
    statusIndicator?: SidebarRuleOperatorStatusIndicator;
}

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
