import React from "react";
import { Edge, Elements, FlowElement, OnLoadParams, removeElements } from "react-flow-renderer";
import { RuleEditorModelContext } from "./contexts/RuleEditorModelContext";
import { RuleEditorContext, RuleEditorContextProps } from "./contexts/RuleEditorContext";
import ruleEditorUtils from "./RuleEditor.utils";
import { useTranslation } from "react-i18next";
import { DeleteNodeAction, IRuleModelEditOperation } from "./RuleEditor.typings";

export interface RuleEditorModelProps {
    /** The children that work on this rule model. */
    children: JSX.Element | JSX.Element[];
}

/** The actual rule model, i.e. the model that is displayed in the editor.
 *  All rule model changes must happen here. */
export const RuleEditorModel = <ITEM_TYPE extends object>({ children }: RuleEditorModelProps) => {
    const { t } = useTranslation();
    /** If set, then the model cannot be modified. */
    const [isReadOnly, setIsReadOnly] = React.useState<boolean>(false);
    const [reactFlowInstance, setReactFlowInstance] = React.useState<OnLoadParams | undefined>(undefined);
    /** The nodes and edges of the rule editor. */
    const [elements, setElements] = React.useState<Elements>([]);
    // Contexts
    const ruleEditorContext = React.useContext<RuleEditorContextProps>(RuleEditorContext);
    const [ruleEditStack, setRuleEditStack] = React.useState<IRuleModelEditOperation[]>([]);

    /** Starts a new change transaction than can be undone/redone in one step. */
    const startChangeTransaction = () => {
        ruleEditStack.push({ type: "transaction start" });
    };

    /** Functions that change the model state. */ // TODO: Add undo/redo to these functions
    // Delete multiple elements and return updated elements array
    const deleteElements = (elementsToRemove, els: Array<FlowElement>) => {
        return removeElements(elementsToRemove, els);
    };

    /** Delete multiple nodes, e.g. from a selection. */
    const deleteNodes = (nodeIds: string[]) => {
        const nodeIdSet = new Set(nodeIds);
        setElements((els) => {
            const nodes = els.filter((n) => ruleEditorUtils.isNode(n) && nodeIdSet.has(n.id));
            if (nodes.length > 0) {
                return deleteElements(nodes, els);
            } else {
                return els;
            }
        });
    };

    /** Deletes a single rule node. */
    const deleteNode = (nodeId: string) => {
        setElements((els) => {
            const node = els.find((n) => ruleEditorUtils.isNode(n) && n.id === nodeId);
            if (node) {
                return deleteElements([node], els);
            } else {
                return els;
            }
        });
    };

    const executeModelEditOperation = (modelEditOperation: IRuleModelEditOperation) => {
        switch (modelEditOperation.type) {
            case "transaction start":
                return startChangeTransaction();
            case "delete node":
                return deleteNode((modelEditOperation as DeleteNodeAction).nodeId);
            default:
                console.error("Tried to execute unsupported operation: " + modelEditOperation.type);
        }
    };

    const saveRule = () => {
        // TODO: Convert react-flow nodes to IRuleOperatorNodes
        return ruleEditorContext.saveRule([]);
    };

    /** Convert initial operator nodes to react-flow model. */
    React.useEffect(() => {
        if (
            ruleEditorContext.initialRuleOperatorNodes &&
            ruleEditorContext.operatorList &&
            elements.length === 0 &&
            reactFlowInstance
        ) {
            const operatorsNodes = ruleEditorContext.initialRuleOperatorNodes;
            // Create nodes
            const nodes = operatorsNodes.map((operatorNode) => {
                return ruleEditorUtils.createOperatorNode(operatorNode, reactFlowInstance, deleteNode, t);
            });
            // Create edges
            const edges: Edge[] = [];
            operatorsNodes.forEach((node) => {
                node.inputs.forEach((inputNodeId, idx) => {
                    if (inputNodeId) {
                        // Edge IDs do not currently matter
                        edges.push(ruleEditorUtils.createEdge(inputNodeId, node.nodeId, `${idx}`));
                    }
                });
            });
            setElements([...nodes, ...edges]);
            setRuleEditStack([]);
        }
    }, [ruleEditorContext.initialRuleOperatorNodes, ruleEditorContext.operatorList, elements, reactFlowInstance]);

    return (
        <RuleEditorModelContext.Provider
            value={{
                elements,
                isReadOnly,
                setIsReadOnly,
                setReactFlowInstance,
                saveRule,
                executeModelEditOperation,
            }}
        >
            {children}
        </RuleEditorModelContext.Provider>
    );
};
