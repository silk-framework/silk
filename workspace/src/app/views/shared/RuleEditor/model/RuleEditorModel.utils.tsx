import React from "react";
import { IHandleProps } from "gui-elements/src/extensions/react-flow/nodes/NodeDefault";
import { ArrowHeadType, Edge, FlowElement, OnLoadParams, Position } from "react-flow-renderer";
import { rangeArray } from "../../../../utils/basicUtils";
import {
    IParameterSpecification,
    IRuleNodeData,
    IRuleOperatorNode,
    NodeContentPropsWithBusinessData,
    RuleOperatorNodeParameters,
    RuleOperatorPluginType,
} from "../RuleEditor.typings";
import { RuleNodeMenu } from "../view/ruleNode/RuleNodeMenu";
import { RuleEditorNode, RuleEditorNodeParameterValue } from "./RuleEditorModel.typings";
import { Connection, Elements, XYPosition } from "react-flow-renderer/dist/types";
import ELK, { ElkNode } from "elkjs";
import { NodeContent, RuleNodeContentProps } from "../view/ruleNode/NodeContent";
import { IconButton } from "gui-elements";

/** Constants */

// Source handle type
export const SOURCE_HANDLE_TYPE = "source";
// Target handle types
export const TARGET_HANDLE_TYPE = "target";

/** Creates a new input handle. Handle IDs need to be numbers that are unique for the same node. */
function createInputHandle(handleId: number, operatorContext?: IOperatorCreateContext): IHandleProps {
    return {
        id: `${handleId}`,
        type: TARGET_HANDLE_TYPE,
        position: Position.Left,
        isValidConnection: operatorContext?.isValidConnection,
    };
}

/** Creates a number of new input handles numbered through by index, i.e. starting with 0. */
function createInputHandles(numberOfInputPorts: number, operatorContext?: IOperatorCreateContext) {
    return rangeArray(numberOfInputPorts).map((nr) => createInputHandle(nr, operatorContext));
}

/** The operations on a node. */
export interface IOperatorNodeOperations {
    handleDeleteNode: (nodeId: string) => any;
    handleParameterChange: (nodeId: string, parameterId: string, value: string) => any;
}

/** Contains all additional items needed for creating an operator. */
export interface IOperatorCreateContext {
    // Operator specification map
    operatorParameterSpecification: Map<string, IParameterSpecification>;
    // Translation function
    t: (string) => string;
    // react-flow instance
    reactFlowInstance: OnLoadParams;
    // Fetches the current value of a node parameter
    currentValue: (nodeId: string, parameterId: string) => RuleEditorNodeParameterValue;
    // Initialize node parameters
    initParameters: (nodeId: string, parameters: RuleOperatorNodeParameters) => any;
    // Returns true if this is a valid connection
    isValidConnection: (connection: Connection) => boolean;
    // The plugin ID of a node
    nodePluginId: (nodeId: string) => string | undefined;
}

/** Creates a new react-flow rule operator node. */
function createOperatorNode(
    node: IRuleOperatorNode,
    nodeOperations: IOperatorNodeOperations,
    operatorContext: IOperatorCreateContext
): RuleEditorNode {
    operatorContext.initParameters(node.nodeId, node.parameters);
    const position = operatorContext.reactFlowInstance.project({
        x: node.position?.x ?? 0,
        y: node.position?.y ?? 0,
    });
    const usedInputs = node.inputs.length;
    const numberOfInputPorts =
        node.portSpecification.maxInputPorts != null
            ? Math.max(node.portSpecification.maxInputPorts, node.portSpecification.minInputPorts, usedInputs)
            : Math.max(node.portSpecification.minInputPorts, usedInputs + 1);

    const handles: IHandleProps[] = [
        ...createInputHandles(numberOfInputPorts, operatorContext),
        { type: SOURCE_HANDLE_TYPE, position: Position.Right, isValidConnection: operatorContext.isValidConnection },
    ];

    const data: NodeContentPropsWithBusinessData<IRuleNodeData> = {
        size: "medium",
        label: node.label,
        minimalShape: "none",
        handles,
        iconName: node.icon, // findExistingIconName(createIconNameStack("FIXME", node.pluginId)), // FIXME: Calculate icons CMEM-3919
        businessData: {
            originalRuleOperatorNode: node,
            dynamicPorts: node.portSpecification.maxInputPorts == null,
        },
        menuButtons: (
            <RuleNodeMenu
                nodeId={node.nodeId}
                t={operatorContext.t}
                handleDeleteNode={nodeOperations.handleDeleteNode}
            />
        ),
        executionButtons: (adjustedContentProps, setAdjustedContentProps) => {
            return (
                <IconButton
                    name={"item-info"}
                    onClick={() => {
                        setAdjustedContentProps({
                            showEditModal: true,
                            onCloseEditModal: () =>
                                setAdjustedContentProps((adjustedProps) => {
                                    // Remove adjusted props again
                                    const { showEditModal, onCloseEditModal, ...otherProps } = adjustedProps;
                                    return {
                                        ...otherProps,
                                    };
                                }),
                        });
                    }}
                />
            );
        },
        content: (adjustedProps: Partial<RuleNodeContentProps>) => (
            <NodeContent
                nodeId={node.nodeId}
                nodeLabel={node.label}
                tags={node.tags}
                operatorContext={operatorContext}
                nodeOperations={nodeOperations}
                nodeParameters={node.parameters}
                showEditModal={false}
                {...adjustedProps}
            />
        ),
    };

    return {
        id: node.nodeId,
        type: nodeType(node.pluginType, node.pluginId),
        position,
        data,
    };
}

const nodeType = (pluginType: RuleOperatorPluginType | string, pluginId: string) => {
    switch (pluginType) {
        case "AggregationOperator":
            return "aggregator";
        case "ComparisonOperator":
            return "comparator";
        case "TransformOperator":
            return "transformation";
        case "PathInputOperator":
            return pluginId === "targetPathInput" ? "targetpath" : "sourcepath";
        default:
            return "default";
    }
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
        nodeOperations: IOperatorNodeOperations,
        operatorContext: IOperatorCreateContext
    ): RuleEditorNode => {
        return createOperatorNode(
            { ...newNode, nodeId: freshNodeId(newNode.pluginId) },
            nodeOperations,
            operatorContext
        );
    };
    return { createNewOperatorNode, initNodeBaseIds, freshNodeId };
};

/** Factory for the createEdge function, since it depends on the edgeCounter state. */
const createEdgeFactory = () => {
    // At the moment edge IDs are not important for us and can always be re-computed
    let edgeCounter = 0;

    /** Creates a new edge. */
    return function createEdge(
        sourceNodeId: string,
        targetNodeId: string,
        targetHandleId: string,
        edgeType: string
    ): Edge {
        edgeCounter += 1;
        return {
            id: `${edgeCounter}`,
            source: sourceNodeId,
            target: targetNodeId,
            type: edgeType,
            targetHandle: targetHandleId,
            arrowHeadType: ArrowHeadType.ArrowClosed,
        };
    };
};

// Helper methods for nodes and edges
const isNode = (element: FlowElement & { source?: string }): boolean => !element.source;
const asNode = (element: FlowElement | undefined): RuleEditorNode | undefined => {
    return element && isNode(element) ? (element as RuleEditorNode) : undefined;
};
const isEdge = (element: FlowElement & { source?: string }): boolean => !isNode(element);
const asEdge = (element: FlowElement | undefined): Edge | undefined => {
    return element && isEdge(element) ? (element as Edge) : undefined;
};

/** Return input handles. */
const inputHandles = (node: RuleEditorNode) => {
    const handles = node.data?.handles ?? [];
    return handles.filter((h) => h.type === TARGET_HANDLE_TYPE && !h.category);
};

/** Return input handles. */
const outputHandles = (node: RuleEditorNode) => {
    const handles = node.data?.handles ?? [];
    return handles.filter((h) => h.type === SOURCE_HANDLE_TYPE);
};

/** Return all other handles than the normal input handles. */
const nonInputHandles = (node: RuleEditorNode) => {
    const handles = node.data?.handles ?? [];
    const nonInputHandles = handles.filter((h) => h.type !== TARGET_HANDLE_TYPE || h.category);
    return nonInputHandles;
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

const edgesById = (elements: Elements, edgeIds: string[]): Edge[] => {
    const edgeIdSet = new Set(edgeIds);
    return elements.filter((elem) => isEdge(elem) && edgeIdSet.has(asEdge(elem)!!.id)).map((elem) => asEdge(elem)!!);
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
const buildElkGraph = (elements: Elements, zoomFactor: number): ElkNode => {
    const nodes = elementNodes(elements);
    const edges = elementEdges(elements);
    const maxNodeIndex = new Map<string, number>();
    edges.forEach((edge) => {
        const currentIdx = maxNodeIndex.get(edge.source) ?? -1;
        const edgeIdx = Number.parseInt(edge.targetHandle ?? "-1") + 1;
        if (edgeIdx > currentIdx) {
            maxNodeIndex.set(edge.source, edgeIdx);
        }
    });
    const sizes = nodeSizes(zoomFactor);
    const constructElkNode = (node: RuleEditorNode): ElkNode => {
        return {
            id: node.id,
            height: sizes.get(node.id)?.height ?? 100,
            width: sizes.get(node.id)?.width ?? 250,
            y: maxNodeIndex.get(node.id) ?? 0,
        };
    };
    const elkGraph = {
        id: " root node ",
        children: nodes.map((node) => constructElkNode(node)),
        edges: edges.map((edge, idx) => ({
            id: `e${idx}`,
            sources: [edge.source],
            targets: [edge.target],
            type: "DIRECTED",
        })),
    };
    return elkGraph;
};

type Size = { width: number; height: number };

const reactFlowNodeClass = "react-flow__node";
const reactFlowNodeId = "data-id";

/** Get the actual sizes of the nodes in the DOM corrected by the zoom-factor. */
const nodeSizes = (zoomFactor: number): Map<string, Size> => {
    const htmlNodes = document.getElementsByClassName(reactFlowNodeClass);
    const sizes = new Map<string, Size>();
    for (let i = 0; i < htmlNodes.length; i++) {
        const node = htmlNodes[i];
        if (node && node.getAttribute(reactFlowNodeId)) {
            const nodeId = node.getAttribute(reactFlowNodeId);
            const { width, height } = node.getBoundingClientRect();
            sizes.set(nodeId!!, { width: width / zoomFactor, height: height / zoomFactor });
        }
    }
    return sizes;
};

interface FindEdgeParameters {
    elements: Elements;
    source?: string;
    target?: string;
    targetHandle?: string;
}
/** Finds all edges matching the given parameters. */
const findEdges = ({ elements, source, target, targetHandle }: FindEdgeParameters): Edge[] => {
    const matchingEdges = elements.filter((elem) => {
        if (isEdge(elem)) {
            const edge = asEdge(elem)!!;
            return (
                (!source || edge.source === source) &&
                (!target || edge.target === target) &&
                (!targetHandle || edge.targetHandle === targetHandle)
            );
        } else {
            return false;
        }
    });
    return matchingEdges as Edge[];
};

/** Layouts the nodes of the rule graph.
 *
 * Returns a map of the new node positions. */
const autoLayout = async (elements: Elements, zoomFactor: number): Promise<Map<string, XYPosition>> => {
    const elkGraph = buildElkGraph(elements, zoomFactor);
    const layoutedGraph = await elk.layout(elkGraph, {
        layoutOptions: {
            "elk.algorithm": "layered",
            "elk.edgeRouting": "POLYLINE", // for kind of symmetrical/centered tree
            "elk.direction": "RIGHT",
            "elk.layered.spacing.nodeNodeBetweenLayers": "100",
            "spacing.nodeNode": "40",
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

/** Removes trailing undefined inputs from the array inline. */
const adaptInputArray = (inputArray: (string | undefined)[]) => {
    if (inputArray.length > 0 && inputArray[inputArray.length - 1] == null) {
        // Need to adapt array
        let idx = inputArray.length - 1;
        while (idx > 0 && inputArray[idx - 1] == null) {
            idx -= 1;
        }
        inputArray.splice(idx, inputArray.length - idx);
    }
};

export const ruleEditorModelUtilsFactory = (
    edgeType: (sourceNodeId: string, targetNode: string) => string = () => "step"
) => {
    const { createNewOperatorNode, initNodeBaseIds, freshNodeId } = initNodeBaseIdsFactory();
    return {
        adaptInputArray,
        asEdge,
        asNode,
        autoLayout,
        createEdge: createEdgeFactory(),
        createInputHandle,
        createInputHandles,
        createNewOperatorNode,
        createOperatorNode,
        edgeById,
        edgesById,
        elementNodes,
        elementEdges,
        findEdges,
        initNodeBaseIds,
        inputHandles,
        outputHandles,
        nonInputHandles,
        isNode,
        isEdge,
        freshNodeId,
        nodeById,
        nodesById,
    };
};
