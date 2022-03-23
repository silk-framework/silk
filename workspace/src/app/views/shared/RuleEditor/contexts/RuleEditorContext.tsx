import React from "react";
import {
    IParameterSpecification,
    IRuleOperator,
    IRuleOperatorNode,
    IRuleSidebarPreConfiguredOperatorsTabConfig,
    IRuleSideBarFilterTabConfig,
} from "../RuleEditor.typings";
import { IViewActions } from "../../../plugins/PluginRegistry";

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
    saveRule: (ruleOperatorNodes: IRuleOperatorNode[]) => Promise<boolean> | boolean;
    /** Converts a rule operator to a rule node. */
    convertRuleOperatorToRuleNode: (ruleOperator: IRuleOperator) => Omit<IRuleOperatorNode, "nodeId">;
    /** Validate a connection. Specifies which connections are allowed between nodes. */
    validateConnection: (
        fromRuleOperatorNode: IRuleOperatorNode,
        toRuleOperatorNode: IRuleOperatorNode,
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
});
