import { Edge, Node } from "@xyflow/react";
import { ReactFlowElement } from "./typing";

/** The elements separated into nodes and edges as its usual starting from react-flow-renderer v10.x */
interface ReactFlowElements<T extends Record<string, unknown>> {
    nodes: Node<T>[];
    edges: Edge[];
}

const isNode = (element: ReactFlowElement & { source?: string }): boolean => !element.source;

/** Remove elements from another list of elements. All edges of removed nodes are also removed. */
const removeElements = <T extends Record<string, unknown>>(
    elementsToRemove: ReactFlowElement[],
    elements: ReactFlowElements<T>,
): ReactFlowElements<T> => {
    const newElements = { ...elements };
    const nodesToRemove = new Set<string>();
    const edgesToRemove = new Set<string>();
    elementsToRemove.forEach((elem) => {
        if (isNode(elem)) {
            nodesToRemove.add(elem.id);
        } else {
            edgesToRemove.add(elem.id);
        }
    });
    if (nodesToRemove.size) {
        newElements.nodes = elements.nodes.filter((node) => !nodesToRemove.has(node.id));
    }
    if (edgesToRemove.size) {
        newElements.edges = elements.edges.filter(
            (edge) => !edgesToRemove.has(edge.id) && !nodesToRemove.has(edge.source) && !nodesToRemove.has(edge.target),
        );
    }
    return newElements;
};

/** Remove a list of edges from another list of edges. */
const removeEdges = (edgesToRemove: Edge[], edges: Edge[]): Edge[] => {
    const edgesToRemoveSet = new Set<string>();
    edgesToRemove.forEach((edge) => edgesToRemoveSet.add(edge.id));
    if (edgesToRemoveSet.size) {
        return edges.filter((edge) => !edgesToRemoveSet.has(edge.id));
    } else {
        return [...edges];
    }
};

const utils = {
    isNode,
    removeElements,
    removeEdges,
};

export default utils;
