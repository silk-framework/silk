import React from "react";
import ReactFlow, {
    OnLoadParams,
    ReactFlowProvider,
    Elements,
    XYPosition,
    addEdge,
} from "react-flow-renderer";
import FlowTooltip from "./components/Tooltip.components";
import { DoubleSlotNode } from "./CustomNodes";
import SideBar from "./SideBar";

export interface IWorkflowEditorProps {
    baseUrl: string;
    projectId: string;
    workflowTaskId: string;
}

let id = 0;
const getId = () => `dndnode_${id++}`;

const nodeTypes = {
    customNode: DoubleSlotNode,
};

/**************** context ****************/
export const FlowContext = React.createContext<any>({});

/**************** reducer types ****************/
interface State {
    reactFlowInstance: OnLoadParams | undefined;
    elements: Elements;
    toolTip: {
        position: XYPosition;
        content: string;
    } | null;
}

export enum ACTION_TYPES {
    setInstance = "SET_INSTANCE",
    addElements = "ADD_ELEMENTS",
    setElements = "SET_ELEMENTS",
    activateTooltip = "ACTIVATE_TOOLTIP",
}

interface Action {
    type: ACTION_TYPES;
    payload: any;
}

/*************** reducer function for managing state between react flow and custom nodes ****************/
const flowReducer = (state: State, action: Action): State => {
    switch (action.type) {
        case ACTION_TYPES.setInstance:
            return {
                ...state,
                reactFlowInstance: action.payload,
            };
        case ACTION_TYPES.addElements:
            return {
                ...state,
                elements: [...state.elements, action.payload],
            };
        case ACTION_TYPES.setElements:
            return {
                ...state,
                elements: action.payload,
            };
        case ACTION_TYPES.activateTooltip:
            return {
                ...state,
                toolTip: action.payload,
            };

        default:
            return state;
    }
};

const INITIAL_STATE: State = {
    reactFlowInstance: undefined,
    elements: [],
    toolTip: null,
};

export function WorkflowEditor({
    baseUrl,
    projectId,
    workflowTaskId,
}: IWorkflowEditorProps) {
    const reactFlowWrapper = React.useRef<any>(null);
    const [state, dispatch] = React.useReducer(flowReducer, INITIAL_STATE);
    const onConnect = (params) => {
        dispatch({
            type: ACTION_TYPES.setElements,
            payload: addEdge(params, state.elements),
        });
    };

    const onLoad = (_reactFlowInstance: OnLoadParams) => {
        dispatch({
            type: ACTION_TYPES.setInstance,
            payload: _reactFlowInstance,
        });
    };

    const onDragOver = (event) => {
        event.preventDefault();
        event.dataTransfer.dropEffect = "move";
    };

    const onDrop = (event) => {
        event.preventDefault();

        const reactFlowBounds =
            reactFlowWrapper.current?.getBoundingClientRect();
        const stringData = event.dataTransfer.getData("application/reactflow");
        const { nodeType: type, label } = JSON.parse(stringData);

        const position = state.reactFlowInstance?.project({
            x: event.clientX - reactFlowBounds.left,
            y: event.clientY - reactFlowBounds.top,
        });

        const newNode = {
            id: getId(),
            type,
            position,
            data: { label, invalid: true },
        };
        dispatch({ type: ACTION_TYPES.addElements, payload: newNode });
    };

    return (
        <FlowContext.Provider value={{ ...state, dispatch }}>
            <div className="land-here">
                <ReactFlowProvider>
                    <SideBar />
                    <div ref={reactFlowWrapper} className="reactflow-wrapper">
                        <ReactFlow
                            onConnect={onConnect}
                            elements={state.elements}
                            onLoad={onLoad}
                            onDrop={onDrop}
                            onDragOver={onDragOver}
                            nodeTypes={nodeTypes}
                        />
                    </div>
                    <FlowTooltip />
                </ReactFlowProvider>
            </div>
        </FlowContext.Provider>
    );
}
