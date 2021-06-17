import React from "react";
import ReactFlow, {
    removeElements,
    addEdge,
    Elements,
    Controls,
} from "react-flow-renderer";
import { DoubleSlotNode } from "../CustomNodes";

const onLoad = (reactFlowInstance) => {
    reactFlowInstance.fitView();
};

const initialElements = getElements(5, 100);
const nodeTypes = {
    customNode: DoubleSlotNode,
};

const StressFlow = () => {
    const [elements, setElements] = React.useState<Elements>(initialElements);
    const onElementsRemove = (elementsToRemove) =>
        setElements((els) => removeElements(elementsToRemove, els));
    const onConnect = (params) => setElements((els) => addEdge(params, els));

    return (
        <ReactFlow
            elements={elements}
            onLoad={onLoad}
            onConnect={onConnect}
            onElementsRemove={onElementsRemove}
            nodeTypes={nodeTypes}
        >
            <Controls />
        </ReactFlow>
    );
};

function getElements(xElements = 10, yElements = 10) {
    const initialElements: Array<any> = [];
    let nodeId: number = 1;
    let recentNodeId: number | null = null;

    for (let y = 0; y < yElements; y++) {
        for (let x = 0; x < xElements; x++) {
            const position = { x: x * 100, y: y * 50 };
            const data = { label: `Node ${nodeId}` };
            const node = {
                id: `stress-${nodeId.toString()}`,
                style: { width: 50, fontSize: 11 },
                type: "customNode",
                data,
                position,
            };
            initialElements.push(node);

            if (recentNodeId && nodeId <= xElements * yElements) {
                initialElements.push({
                    id: `${x}-${y}`,
                    source: `stress-${recentNodeId.toString()}`,
                    target: `stress-${nodeId.toString()}`,
                });
            }

            recentNodeId = nodeId;
            nodeId++;
        }
    }

    return initialElements;
}

export default StressFlow;
