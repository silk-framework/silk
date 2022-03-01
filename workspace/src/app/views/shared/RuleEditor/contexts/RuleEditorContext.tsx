import React from "react";
import { IParameterSpecification, IRuleOperator, IRuleOperatorNode } from "../RuleEditor.typings";

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
