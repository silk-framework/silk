import React from "react";
import {
    IParameterSpecification,
    IRuleOperator,
    IRuleOperatorNode,
    IRuleSidebarPreConfiguredOperatorsTabConfig,
    IRuleSideBarFilterTabConfig,
    RuleSaveResult,
    RuleEditorValidationNode,
    PathMetaDataFunctions,
} from "../RuleEditor.typings";
import { IViewActions } from "../../../plugins/PluginRegistry";
import { DatasetCharacteristics } from "../../typings";
import { StickyNote } from "@eccenca/gui-elements";
import { IPartialAutoCompleteResult } from "@eccenca/gui-elements/src/components/AutoSuggestion/AutoSuggestion";
import { InitialRuleHighlighting } from "../../../taskViews/transform/transform.types";
import { RuleEditorBaseProps } from "../RuleEditor";

/**
 * The rule editor context that contains objects and methods related to the original objects that are being edited and
 * the operators that can are available.
 *
 * @param ITEM_TYPE The interface of the rule based item that is being edited.
 * @param OPERATOR_TYPE The interface of the operators that can be placed in the editor.
 */
export interface RuleEditorContextProps extends RuleEditorBaseProps {
    /** Unique ID for the edited item. This needs to be unique inside the project. */
    editedItemId?: string;
    /** The item whose rules are being edited, e.g. linking or transformation. */
    editedItem?: object;
    /** The operators that can be dragged and dropped onto the rule editor. */
    operatorList?: IRuleOperator[];
    /** The operator parameter specification of each operator plugin. */
    operatorSpec?: Map<string, Map<string, IParameterSpecification>>;
    /** Loading states. */
    editedItemLoading: boolean;
    /** If the operator list is still loading. */
    operatorListLoading: boolean;
    /** The initial rule nodes, e.g. when loading an existing rule. */
    initialRuleOperatorNodes?: IRuleOperatorNode[];
    /** Save the rule. */
    saveRule: (
        ruleOperatorNodes: IRuleOperatorNode[],
        stickyNotes?: StickyNote[],
    ) => Promise<RuleSaveResult> | RuleSaveResult;
    /** Converts a rule operator to a rule node. */
    convertRuleOperatorToRuleNode: (ruleOperator: IRuleOperator) => Omit<IRuleOperatorNode, "nodeId">;
    /** If set to true the editor will be in read-only mode and cannot be set into edit mode. */
    readOnlyMode?: boolean;
    /** The last save result. */
    lastSaveResult?: RuleSaveResult;
    /** UI annotation sticky notes */
    stickyNotes: StickyNote[];
    /** Dataset characteristics, e.g. used for the 'PathInputOperator' type. The key is the corresponding plugin ID. */
    datasetCharacteristics: Map<string, DatasetCharacteristics>;
}

/** Creates a rule editor model context that contains the actual rule model and low-level update functions. */
export const RuleEditorContext = React.createContext<RuleEditorContextProps>({
    projectId: "",
    editedItemLoading: false,
    operatorListLoading: false,
    saveRule: () => {
        throw Error("saveRule is not implemented!");
    },
    convertRuleOperatorToRuleNode: () => {
        throw Error("convertRuleOperatorToRuleNode is not implemented!");
    },
    validateConnection: () => true,
    stickyNotes: [],
    showRuleOnly: false,
    hideMinimap: false,
    zoomRange: [0.25, 1.5],
    initialFitToViewZoomLevel: 0.75,
    instanceId: "uniqueId",
    datasetCharacteristics: new Map(),
    pathMetaData: {
        inputPathPluginPathType: () => undefined,
        inputPathLabel: () => undefined,
    },
    partialAutoCompletion: () => async () => undefined,
    saveInitiallyEnabled: false,
});
