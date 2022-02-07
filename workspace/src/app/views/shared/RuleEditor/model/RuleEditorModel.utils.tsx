import React from "react";
import { IHandleProps } from "gui-elements/src/extensions/react-flow/nodes/NodeDefault";
import { ArrowHeadType, Edge, FlowElement, Node, OnLoadParams, Position } from "react-flow-renderer";
import { rangeArray } from "../../../../utils/basicUtils";
import { IRuleNodeData, IRuleOperatorNode, NodeContentPropsWithBusinessData } from "../RuleEditor.typings";
import { RuleNodeMenu } from "../ruleNode/RuleNodeMenu";
import { Tag, Highlighter, Spacing } from "gui-elements";
import { RuleEditorNode } from "./RuleEditorModel.typings";
import { Elements, XYPosition } from "react-flow-renderer/dist/types";
import ELK, { ElkNode } from "elkjs";

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
function createOperatorNode(
    node: IRuleOperatorNode,
    reactFlowInstance: OnLoadParams,
    handleDeleteNode: (nodeId: string) => any,
    t: (string) => string
): RuleEditorNode {
    const position = reactFlowInstance.project({
        x: node.position?.x ?? 0, // FIXME: Calculate position based on  algorithm when coordinates are missing CMEM-3922
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
        iconName: node.icon, // findExistingIconName(createIconNameStack("FIXME", node.pluginId)), // FIXME: Calculate icons CMEM-3919
        businessData: {
            originalRuleOperatorNode: node,
            dynamicPorts: !node.portSpecification.maxInputPorts,
        },
        menuButtons: <RuleNodeMenu nodeId={node.nodeId} t={t} handleDeleteNode={handleDeleteNode} />,
        content: node.tags ? createTags(node.tags) : null,
    };

    return {
        id: node.nodeId,
        type: "default", // FIXME: Set node type here CMEM-3919
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

/**
 * Returns the createNewOperatorNode, initNodeBaseIds and freshNodeId functions.
 */
const initNodeBaseIdsFactory = () => {
    const nodeBaseIdCounter: Map<string, number | undefined> = new Map();

    /** Init the node base IDs based on the provided elements. */
    const initNodeBaseIds = (elements: Elements): void => {
        nodeBaseIdCounter.clear();
        elements.forEach((elem) => {
            if (isNode(elem)) {
                const separatorIdx = elem.id.lastIndexOf("_");
                const parseSuffix = (): number => parseInt(elem.id.substr(separatorIdx + 1));
                if (separatorIdx > 0 && !isNaN(parseSuffix())) {
                    const suffix = parseSuffix();
                    const base = elem.id.substr(0, separatorIdx);
                    const currentSuffix = nodeBaseIdCounter.get(base) ?? 0;
                    if (suffix > currentSuffix) {
                        nodeBaseIdCounter.set(base, suffix);
                    }
                } else {
                    // Only set if no other entry exists, since this must be already at least as high
                    if (!nodeBaseIdCounter.has(elem.id)) {
                        nodeBaseIdCounter.set(elem.id, 1);
                    }
                }
            }
        });
    };
    /** Generates an unused node ID based on a base ID */
    const freshNodeId = (baseId: string): string => {
        const currentCount = nodeBaseIdCounter.get(baseId);
        const newCount = (currentCount ?? 0) + 1;
        nodeBaseIdCounter.set(baseId, newCount);
        // Only add number suffix if base ID already exists
        return currentCount !== undefined ? `${baseId}_${newCount}` : baseId;
    };
    const createNewOperatorNode = (
        newNode: Omit<IRuleOperatorNode, "nodeId">,
        reactFlowInstance: OnLoadParams,
        handleDeleteNode: (nodeId: string) => any,
        t: (string) => string
    ): RuleEditorNode => {
        return createOperatorNode(
            { ...newNode, nodeId: freshNodeId(newNode.pluginId) },
            reactFlowInstance,
            handleDeleteNode,
            t
        );
    };
    return { createNewOperatorNode, initNodeBaseIds, freshNodeId };
};

/** Factory for the createEdge function, since it depends on the edgeCounter state. */
const createEdgeFactory = () => {
    // At the moment edge IDs are not important for us and can always be re-computed
    let edgeCounter = 0;

    /** Creates a new edge. */
    return function createEdge(sourceNodeId: string, targetNodeId: string, targetHandleId: string) {
        edgeCounter += 1;
        return {
            id: `${edgeCounter}`,
            source: sourceNodeId,
            target: targetNodeId,
            type: "step",
            targetHandle: targetHandleId,
            arrowHeadType: ArrowHeadType.ArrowClosed,
        };
    };
};

// Helper methods for nodes and edges
const isNode = (element: FlowElement & { source?: string }): boolean => !element.source;
const asNode = (element: FlowElement | undefined): RuleEditorNode | undefined => {
    return element && isNode(element) ? (element as Node<NodeContentPropsWithBusinessData<IRuleNodeData>>) : undefined;
};
const isEdge = (element: FlowElement & { source?: string }): boolean => !isNode(element);
const asEdge = (element: FlowElement | undefined): Edge | undefined => {
    return element && isEdge(element) ? (element as Edge) : undefined;
};

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

const edgeById = (elements: Elements, edgeId: string): Edge | undefined => {
    return asEdge(elements.find((e) => isEdge(e) && e.id === edgeId));
};

/** Returns all nodes. */
const elementNodes = (elements: Elements): RuleEditorNode[] => {
    return elements.filter((elem) => isNode(elem)).map((node) => asNode(node)!!);
};

/** Returns all edges. */
const elementEdges = (elements: Elements): Edge[] => {
    return elements.filter((elem) => isEdge(elem)).map((edge) => asEdge(edge)!!);
};

const elk = new ELK();

// Builds an ELK graph from the react-flow elements
const buildElkGraph = (elements: Elements): ElkNode => {
    const nodes = elementNodes(elements);
    const edges = elementEdges(elements);
    const constructElkNode = (node: RuleEditorNode): ElkNode => {
        return {
            id: node.id,
            height: 100, // TODO: Set to something meaningful?
            width: 300, // TODO: Set to something meaningful?
        };
    };
    return {
        id: " root node ",
        children: nodes.map((node) => constructElkNode(node)),
        edges: edges.map((edge, idx) => ({
            id: `e${idx}`,
            sources: [edge.source],
            targets: [edge.target],
            type: "DIRECTED",
        })),
    };
};

/** Layouts the nodes of the rule graph.
 *
 * Returns a map of the new node positions. */
const autoLayout = async (elements: Elements): Promise<Map<string, XYPosition>> => {
    const elkGraph = buildElkGraph(elements);
    const layoutedGraph = await elk.layout(elkGraph, {
        layoutOptions: {
            "elk.algorithm": "layered",
            "elk.edgeRouting": "POLYLINE", // for kind of symmetrical/centered tree
            "elk.direction": "RIGHT",
            "elk.padding": "[top=25,left=25,bottom=25,right=25]",
            "elk.spacing.componentComponent": "25",
            "elk.layered.spacing.nodeNodeBetweenLayers": "25",
            "elk.layered.crossingMinimization.semiInteractive": "true", // For the  order of input nodes
            "elk.layered.crossingMinimization.strategy": "INTERACTIVE",
        },
    });
    const nodePositions = new Map<string, XYPosition>();
    (layoutedGraph.children ?? []).forEach((layoutedElkNode) => {
        if (layoutedElkNode.x != null && layoutedElkNode.y != null) {
            nodePositions.set(layoutedElkNode.id, { x: layoutedElkNode.x, y: layoutedElkNode.y });
        }
    });
    return nodePositions;
};

export const ruleEditorModelUtilsFactory = () => {
    const { createNewOperatorNode, initNodeBaseIds, freshNodeId } = initNodeBaseIdsFactory();
    return {
        asEdge,
        asNode,
        autoLayout,
        createEdge: createEdgeFactory(),
        createInputHandle,
        createInputHandles,
        createNewOperatorNode,
        createOperatorNode,
        edgeById,
        initNodeBaseIds,
        inputHandles,
        isNode,
        isEdge,
        freshNodeId,
        nodeById,
        nodesById,
    };
};
