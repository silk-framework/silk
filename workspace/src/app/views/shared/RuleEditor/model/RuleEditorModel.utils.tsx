import React from "react";
import { IHandleProps } from "@eccenca/gui-elements/src/extensions/react-flow/nodes/NodeContent";
import { ArrowHeadType, Edge, FlowElement, Position } from "react-flow-renderer";
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
import dagre from "dagre";
import { NodeContent, RuleNodeContentProps } from "../view/ruleNode/NodeContent";
import { IconButton } from "@eccenca/gui-elements";
import { RuleEditorEvaluationContextProps } from "../contexts/RuleEditorEvaluationContext";
import {LanguageFilterProps} from "../view/ruleNode/PathInputOperator";

/** Constants */

export const DEFAULT_NODE_HEIGHT = 70;
export const DEFAULT_NODE_WIDTH = 240;

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
    handleCloneNode: (nodeId: string) => any;
    handleParameterChange: (nodeId: string, parameterId: string, value: RuleEditorNodeParameterValue) => any;
}

/** Contains all additional items needed for creating an operator. */
export interface IOperatorCreateContext {
    // Operator specification map
    operatorParameterSpecification: Map<string, IParameterSpecification>;
    // Translation function
    t: (string) => string;
    // Fetches the current value of a node parameter
    currentValue: (nodeId: string, parameterId: string) => RuleEditorNodeParameterValue;
    // Initialize node parameters
    initParameters: (nodeId: string, parameters: RuleOperatorNodeParameters) => any;
    // Returns true if this is a valid connection
    isValidConnection: (connection: Connection) => boolean;
    // The plugin ID of a node
    nodePluginId: (nodeId: string) => string | undefined;
    // Rule evaluation context
    ruleEvaluationContext: RuleEditorEvaluationContextProps;
    // Updates several node parameters in a single transaction
    updateNodeParameters: (nodeId: string, parameterValues: Map<string, RuleEditorNodeParameterValue>) => any;
    // If the operator is in permanent read-only mode
    readOnlyMode: boolean;
    /** If for this operator there is a language filter supported. Currently only path operators are affected by this option. */
    languageFilterEnabled: (nodeId: string) => LanguageFilterProps | undefined;
}

/** Creates a new react-flow rule operator node. */
function createOperatorNode(
    node: IRuleOperatorNode,
    nodeOperations: IOperatorNodeOperations,
    operatorContext: IOperatorCreateContext
): RuleEditorNode {
    operatorContext.initParameters(node.nodeId, node.parameters);
    const position = {
        x: node.position?.x ?? 0,
        y: node.position?.y ?? 0,
    };
    const usedInputs = node.inputs.length;
    const numberOfInputPorts =
        node.portSpecification.maxInputPorts != null
            ? Math.max(node.portSpecification.maxInputPorts, node.portSpecification.minInputPorts, usedInputs)
            : Math.max(node.portSpecification.minInputPorts, usedInputs + 1);

    const handles: IHandleProps[] = [
        ...createInputHandles(numberOfInputPorts, operatorContext),
        { type: SOURCE_HANDLE_TYPE, position: Position.Right, isValidConnection: operatorContext.isValidConnection },
    ];

    const editBtn = (setAdjustedContentProps: React.Dispatch<React.SetStateAction<Partial<RuleNodeContentProps>>>) => (
        <IconButton
            name={operatorContext.readOnlyMode ? "item-viewdetails" : "item-edit"}
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
            text={operatorContext.t("RuleEditor.node.executionButtons.edit.tooltip")}
        />
    );

    const data: NodeContentPropsWithBusinessData<IRuleNodeData> = {
        size: "medium",
        label: node.label,
        minimalShape: "none",
        handles,
        iconName: node.icon,
        businessData: {
            originalRuleOperatorNode: node,
            dynamicPorts: node.portSpecification.maxInputPorts == null,
        },
        menuButtons: (
            <RuleNodeMenu
                nodeId={node.nodeId}
                t={operatorContext.t}
                handleDeleteNode={nodeOperations.handleDeleteNode}
                ruleOperatorDescription={node.description}
                ruleOperatorDocumentation={node.markdownDocumentation}
                handleCloneNode={nodeOperations.handleCloneNode}
            />
        ),
        executionButtons:
            Object.keys(node.parameters).length > 0
                ? (adjustedContentProps, setAdjustedContentProps) => {
                      return editBtn(setAdjustedContentProps);
                  }
                : undefined,
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
        contentExtension: operatorContext.ruleEvaluationContext.supportsEvaluation
            ? operatorContext.ruleEvaluationContext.createRuleEditorEvaluationComponent(node.nodeId)
            : undefined,
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

// Layouts the nodes and returns a map with the new coordinates for the nodes
const layoutGraph = (elements: Elements, zoomFactor: number, canvasId: string): Map<string, XYPosition> => {
    const g = new dagre.graphlib.Graph();
    // Init defaults
    g.setGraph({});
    g.setDefaultEdgeLabel(function () {
        return {};
    });
    g.graph().rankDir = "LR";
    g.graph().nodesep = 80;
    g.graph().ranksep = 120;
    const nodes = elementNodes(elements);
    const edges = elementEdges(elements);
    // Sort edges to maintain input order of edges
    edges.sort((a, b) => {
        const aHandle = Number.parseInt(a.targetHandle ?? "-1");
        const bHandle = Number.parseInt(b.targetHandle ?? "-1");
        const smaller = a.target === b.target ? aHandle < bHandle : a.target < b.target;
        return smaller ? -1 : 1;
    });
    const sizes = nodeSizes(zoomFactor, canvasId);
    const addNode = (node: RuleEditorNode) => {
        const defaultHeight = (): number => {
            const parameterCount = Object.values(node.data.businessData.originalRuleOperatorNode.parameters).length;
            return 100 + parameterCount * 75;
        };
        g.setNode(node.id, {
            label: node.id,
            height: sizes.get(node.id)?.height ?? defaultHeight(),
            width: sizes.get(node.id)?.width ?? 250,
        });
    };
    // Add all nodes to the dagre graph
    nodes.forEach((node) => addNode(node));
    // Add edges to the dagre graph
    edges.forEach((edge, idx) => {
        g.setEdge(edge.source, edge.target);
    });
    dagre.layout(g);
    const nodeMap = new Map<string, XYPosition>();
    g.nodes().forEach((nodeId) => {
        const node = g.node(nodeId);
        // dagre computes the position to be the vertical center of the node, react-flow uses the upper-left point, so we need to translate
        nodeMap.set(nodeId, { x: node.x, y: node.y - (sizes.get(nodeId)?.height ?? 0) / 2 });
    });
    return nodeMap;
};

type Size = { width: number; height: number };

const reactFlowNodeClass = "react-flow__node";
const reactFlowNodeId = "data-id";

/** Get the actual sizes of the nodes in the DOM corrected by the zoom-factor. */
const nodeSizes = (zoomFactor: number, canvasId: string): Map<string, Size> => {
    const canvas = document.getElementById(canvasId);
    const htmlNodes = canvas ? canvas.getElementsByClassName(reactFlowNodeClass) : [];
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
const autoLayout = (elements: Elements, zoomFactor: number, canvasId: string): Map<string, XYPosition> => {
    const nodes = elementNodes(elements);
    const center = graphCenter(nodes.filter((n) => n.position).map((n) => n.position));
    const newPositions = layoutGraph(elements, zoomFactor, canvasId);
    // Adapt positions so the graph is not rendered somewhere else, keep the original center
    const newCenter = graphCenter([...newPositions.values()]);
    if (center.x !== newCenter.x || center.y !== newCenter.y) {
        const xOffset = newCenter.x - center.x;
        const yOffset = newCenter.y - center.y;
        [...newPositions.keys()].forEach((nodeId) => {
            const nodePosition = newPositions.get(nodeId)!!;
            newPositions.set(nodeId, { x: nodePosition.x - xOffset, y: nodePosition.y - yOffset });
        });
    }
    return newPositions;
};

/** Returns the weighted center of the nodes.
 */
const graphCenter = (nodePositions: XYPosition[]): XYPosition => {
    if (nodePositions.length === 0) {
        return { x: 0, y: 0 };
    }
    let xSum = 0;
    let ySum = 0;
    nodePositions.forEach((pos) => {
        xSum += pos.x;
        ySum += pos.y;
    });
    return {
        x: Math.floor(xSum / nodePositions.length),
        y: Math.floor(ySum / nodePositions.length),
    };
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
