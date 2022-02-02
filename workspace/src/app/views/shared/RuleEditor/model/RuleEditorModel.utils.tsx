import React from "react";
import { IHandleProps } from "gui-elements/src/extensions/react-flow/nodes/NodeDefault";
import { ArrowHeadType, Edge, FlowElement, Node, OnLoadParams, Position } from "react-flow-renderer";
import { rangeArray } from "../../../../utils/basicUtils";
import { IRuleNodeData, IRuleOperatorNode, NodeContentPropsWithBusinessData } from "../RuleEditor.typings";
import { RuleNodeMenu } from "../ruleNode/RuleNodeMenu";
import { Tag, Highlighter, Spacing } from "gui-elements";
import { RuleEditorNode } from "./RuleEditorModel.typings";
import { Elements } from "react-flow-renderer/dist/types";

/** Constants */

// Source handle type
export const SOURCE_HANDLE_TYPE = "source";
// Target handle types
export const TARGET_HANDLE_TYPE = "target";

/** Creates a new input handle. Handle IDs need to be numbers that are unique for the same node. */
function createInputHandle(handleId: number): IHandleProps {
    return {
        id: `${handleId}`,
        type: TARGET_HANDLE_TYPE,
        position: Position.Left,
    };
}

/** Creates a number of new input handles numbered through by index. */
function createInputHandles(numberOfInputPorts: number) {
    return rangeArray(numberOfInputPorts).map((nr) => createInputHandle(nr));
}

function createNewOperatorNode(
    newNode: Omit<IRuleOperatorNode, "nodeId">,
    reactFlowInstance: OnLoadParams,
    handleDeleteNode: (nodeId: string) => any,
    t: (string) => string,
    elements: Elements
): RuleEditorNode {
    return createOperatorNode(
        { ...newNode, nodeId: freshNodeId(elements, newNode.pluginId) },
        reactFlowInstance,
        handleDeleteNode,
        t
    );
}

/** Creates a new react-flow rule operator node. */
function createOperatorNode(
    node: IRuleOperatorNode,
    reactFlowInstance: OnLoadParams,
    handleDeleteNode: (nodeId: string) => any,
    t: (string) => string
): RuleEditorNode {
    const position = reactFlowInstance.project({
        x: node.position?.x ?? 0, // TODO: Calculate position based on  algorithm when coordinates are missing CMEM-3922
        y: node.position?.y ?? 0,
    });
    const usedInputs = node.inputs.length;
    const numberOfInputPorts =
        node.portSpecification.maxInputPorts != null
            ? Math.max(node.portSpecification.maxInputPorts, node.portSpecification.minInputPorts, usedInputs)
            : Math.max(node.portSpecification.minInputPorts, usedInputs + 1);

    const handles: IHandleProps[] = [
        ...createInputHandles(numberOfInputPorts),
        { type: SOURCE_HANDLE_TYPE, position: Position.Right },
    ];

    const data: NodeContentPropsWithBusinessData<IRuleNodeData> = {
        size: "medium",
        label: node.label,
        minimalShape: "none",
        handles,
        iconName: node.icon, // findExistingIconName(createIconNameStack("TODO", node.pluginId)), // TODO: Calculate icons
        businessData: {
            originalRuleOperatorNode: node,
            dynamicPorts: !node.portSpecification.maxInputPorts,
        },
        menuButtons: <RuleNodeMenu nodeId={node.nodeId} t={t} handleDeleteNode={handleDeleteNode} />,
        content: node.tags ? createTags(node.tags) : null,
    };

    return {
        id: node.nodeId,
        type: "default", // TODO: Set node type here
        position,
        data,
    };
}

/** Adds highlighting to the text if query is non-empty. */
const addHighlighting = (text: string, query?: string): string | JSX.Element => {
    return query ? <Highlighter label={text} searchValue={query} /> : text;
};

const createTags = (tags: string[], query?: string) => {
    return (
        <>
            {tags.map((tag, idx) => {
                return (
                    <>
                        <Tag minimal={true}>{addHighlighting(tag, query)}</Tag>
                        {idx < tags.length + 1 ? <Spacing vertical size="tiny" /> : null}
                    </>
                );
            })}
        </>
    );
};

/** Returns the highest suffix number of a react-flow node with the same base ID.
 * If a node with exactly the base ID exists it returns 0. */
function largestBaseSuffixNumber(elements: Array<FlowElement>, baseId: string): number | undefined {
    let highestSuffix: number | undefined = undefined;
    elements.forEach((elem) => {
        if (isNode(elem) && elem.id.startsWith(baseId)) {
            const suffix = elem.id.substr(baseId.length);
            if (suffix.length === 0) {
                highestSuffix = 1;
            } else {
                // There is an underscore between the base ID and the number
                const numberSuffix = parseInt(suffix.substr(1));
                if (!isNaN(numberSuffix) && (highestSuffix === undefined || highestSuffix < numberSuffix)) {
                    highestSuffix = numberSuffix;
                }
            }
        }
    });
    return highestSuffix;
}

/** Generates an unused node ID based on a base ID */
const freshNodeId = (elements: Elements, baseId: string): string => {
    const currentCount = largestBaseSuffixNumber(elements, baseId);
    return currentCount !== undefined ? `${baseId}_${currentCount + 1}` : baseId;
};

// At the moment edge IDs are not important for us and can always be re-computed
let edgeCounter = 0;

/** Creates a new edge. */
function createEdge(sourceNodeId: string, targetNodeId: string, targetHandleId: string) {
    edgeCounter += 1;
    return {
        id: `${edgeCounter}`,
        source: sourceNodeId,
        target: targetNodeId,
        type: "step",
        targetHandle: targetHandleId,
        arrowHeadType: ArrowHeadType.ArrowClosed,
    };
}

// Helper methods for nodes and edges
const isNode = (element: FlowElement & { source?: string }): boolean => !element.source;
const asNode = (element: FlowElement | undefined): RuleEditorNode | undefined => {
    return element && isNode(element) ? (element as Node<NodeContentPropsWithBusinessData<IRuleNodeData>>) : undefined;
};
const isEdge = (element: FlowElement & { source?: string }): boolean => !isNode(element);
const asEdge = (element: FlowElement): Edge | undefined => (isEdge(element) ? (element as Edge) : undefined);

/** Return inpt handles. */
const inputHandles = (node: RuleEditorNode) => {
    const handles = node.data?.handles ?? [];
    const inputHandles = handles.filter((h) => h.type === "target" && !h.category);
    return inputHandles;
};

/** Find the rule node by ID. */
const nodeById = (elements: Elements, nodeId: string): RuleEditorNode | undefined => {
    return asNode(elements.find((n) => isNode(n) && n.id === nodeId));
};

/** Return all nodes with one of the given IDs. */
const nodesById = (elements: Elements, nodeIds: string[]): RuleEditorNode[] => {
    const nodeIdSet = new Set(nodeIds);
    return elements.filter((n) => isNode(n) && nodeIdSet.has(n.id)).map((n) => asNode(n)!!);
};

const ruleEditorModelUtils = {
    asEdge,
    asNode,
    createEdge,
    createInputHandle,
    createInputHandles,
    createNewOperatorNode,
    createOperatorNode,
    inputHandles,
    isNode,
    isEdge,
    freshNodeId,
    nodeById,
    nodesById,
};

export default ruleEditorModelUtils;
