import React from "react";
import { IHandleProps } from "gui-elements/src/extensions/react-flow/nodes/NodeDefault";
import { ArrowHeadType, Edge, FlowElement, OnLoadParams, Position } from "react-flow-renderer";
import { rangeArray } from "../../../utils/basicUtils";
import { IRuleNodeData, IRuleOperatorNode, NodeContentPropsWithBusinessData } from "./RuleEditor.typings";
import { RuleNodeMenu } from "./ruleNode/RuleNodeMenu";
import { Tag, Highlighter, Spacing } from "gui-elements";

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

/** Creates a new react-flow rule operator node. */
export function createOperatorNode(
    node: IRuleOperatorNode,
    reactFlowInstance: OnLoadParams,
    handleDeleteNode: (nodeId: string) => any,
    t: (string) => string
) {
    const position = reactFlowInstance.project({
        x: node.position?.x ?? 0, // TODO: Calculate position based on  algorithm when coordinates are missing
        y: node.position?.y ?? 0,
    });
    const usedInputs = node.inputs.length;
    const numberOfInputPorts =
        node.portSpecification.maxInputPorts != null
            ? Math.max(node.portSpecification.maxInputPorts, node.portSpecification.minInputPorts, usedInputs)
            : Math.max(node.portSpecification.minInputPorts, usedInputs + 1);

    const handles: IHandleProps[] = [
        ...ruleEditorUtils.createInputHandles(numberOfInputPorts),
        { type: SOURCE_HANDLE_TYPE, position: Position.Right },
    ];

    const data: NodeContentPropsWithBusinessData<IRuleNodeData> = {
        size: "medium",
        label: node.label,
        minimalShape: "none",
        handles,
        iconName: node.icon, // findExistingIconName(createIconNameStack("TODO", node.pluginId)), // TODO: Calculate icons
        businessData: {
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
const isEdge = (element: FlowElement & { source?: string }): boolean => !isNode(element);
const asEdge = (element: FlowElement): Edge | undefined => (isEdge(element) ? (element as Edge) : undefined);

const ruleEditorUtils = {
    asEdge,
    createEdge,
    createInputHandle,
    createInputHandles,
    createOperatorNode,
    isNode,
    isEdge,
};

export default ruleEditorUtils;
