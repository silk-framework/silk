import React from "react";
import {
    IParameterSpecification,
    IRuleOperator,
    IRuleOperatorNode,
    IRuleSidebarPreConfiguredOperatorsTabConfig,
    IRuleSideBarFilterTabConfig,
    RuleSaveResult,
    RuleEditorValidationNode,
} from "../RuleEditor.typings";
import { IViewActions } from "../../../plugins/PluginRegistry";
import { IStickyNote } from "views/taskViews/shared/task.typings";
import { DatasetCharacteristics } from "../../typings";

/**
 * The rule editor context that contains objects and methods related to the original objects that are being edited and
 * the operators that can are available.
 *
 * @param ITEM_TYPE The interface of the rule based item that is being edited.
 * @param OPERATOR_TYPE The interface of the operators that can be placed in the editor.
 */
export interface RuleEditorContextProps {
    /** The project context. */
    projectId: string;
    /** Unique ID for the edited item. This needs to be unique inside the project. */
    editedItemId?: string;
    /** The item whose rules are being edited, e.g. linking or transformation. */
    editedItem?: object;
    /** Optional title that is shown above the toolbar. */
    editorTitle?: string;
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
        stickyNotes?: IStickyNote[]
    ) => Promise<RuleSaveResult> | RuleSaveResult;
    /** Converts a rule operator to a rule node. */
    convertRuleOperatorToRuleNode: (ruleOperator: IRuleOperator) => Omit<IRuleOperatorNode, "nodeId">;
    /** Validate a connection. Specifies which connections are allowed between nodes. */
    validateConnection: (
        fromRuleOperatorNode: RuleEditorValidationNode,
        toRuleOperatorNode: RuleEditorValidationNode,
        targetPortIdx: number
    ) => boolean;
    /** Tabs that allow to show different rule operators or only a subset. The first tab will always be selected first. */
    tabs?: (IRuleSideBarFilterTabConfig | IRuleSidebarPreConfiguredOperatorsTabConfig)[];
    /** Task view actions. */
    viewActions?: IViewActions;
    /** If set to true the editor will be in read-only mode and cannot be set into edit mode. */
    readOnlyMode?: boolean;
    /** Additional components that will be placed in the tool bar left to the save button. */
    additionalToolBarComponents?: () => JSX.Element | JSX.Element[];
    /** The last save result. */
    lastSaveResult?: RuleSaveResult;
    /** UI annotation sticky notes */
    stickyNotes: IStickyNote[];
    /** When enabled only the rule is shown without side- and toolbar and any other means to edit the rule. */
    showRuleOnly?: boolean;
    /** When enabled the mini map is not displayed. */
    hideMinimap?: boolean;
    /** Defines minimum and maximum of the available zoom levels */
    zoomRange?: [number, number];
    /** After the initial fit to view, zoom to the specified Zoom level to avoid showing too small nodes. */
    initialFitToViewZoomLevel?: number;
    /** The ID of the instance. If multiple instances are used in parallel, they need to have unique IDs, else there can be interferences. */
    instanceId: string;
    /** Dataset characteristics, e.g. used for the 'PathInputOperator' type. The key is the corresponding plugin ID. */
    datasetCharacteristics: Map<string, DatasetCharacteristics>;
    /** Returns for a path input plugin and a path the type of the given path. Returns undefined if either the plugin does not exist or the path data is unknown. */
    inputPathPluginPathType?: (inputPathPluginId: string, path: string) => string | undefined;
    /** allow the width of nodes to be adjustable */
    allowFlexibleSize?: boolean;
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
    inputPathPluginPathType: () => undefined,
    allowFlexibleSize: false,
});
