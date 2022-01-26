import React from "react";
import { Edge, Elements, FlowElement, OnLoadParams, removeElements } from "react-flow-renderer";
import { RuleEditorModelContext } from "./contexts/RuleEditorModelContext";
import { RuleEditorContext, RuleEditorContextProps } from "./contexts/RuleEditorContext";
import ruleEditorUtils from "./RuleEditor.utils";
import { useTranslation } from "react-i18next";

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

    /** Functions that change the model state. */ // TODO: Add undo/redo to these functions
    // Delete multiple elements and return updated elements array
    function deleteElements(elementsToRemove, els: Array<FlowElement>) {
        return removeElements(elementsToRemove, els);
    }

    /** Deletes a single rule node. */
    const handleDeleteNode = (nodeId: string) => {
        setElements((els) => {
            const node = els.find((n) => ruleEditorUtils.isNode(n) && n.id === nodeId);
            const newElements = deleteElements([node], els);
            //listen to edge deletions
            return node ? newElements : els;
        });
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
                return ruleEditorUtils.createOperatorNode(operatorNode, reactFlowInstance, handleDeleteNode, t);
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
        }
    }, [ruleEditorContext.initialRuleOperatorNodes, ruleEditorContext.operatorList, elements, reactFlowInstance]);

    return (
        <RuleEditorModelContext.Provider
            value={{
                elements,
                isReadOnly,
                setIsReadOnly,
                setReactFlowInstance,
            }}
        >
            {children}
        </RuleEditorModelContext.Provider>
    );
};
