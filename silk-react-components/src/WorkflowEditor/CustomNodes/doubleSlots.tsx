import { Icon } from "@gui-elements/index";
import React from "react";
import {
    NodeProps,
    Handle,
    Position,
    Connection,
    Edge,
    useStoreState,
    useStoreActions,
} from "react-flow-renderer";
import { ACTION_TYPES, FlowContext } from "../WorkflowEditor";

const style = {
    height: "20px",
    width: "8px",
    borderRadius: "4px",
    zIndex: 2,
};
const onConnect = (params: Connection | Edge) => {
    console.log("custom node onConnect", params);
};

/***
 * 1. onClick / search should highlight node.
 */

const DoubleSlots: React.FC<NodeProps> = ({ data, id, selected }) => {
    const utility = React.useContext(FlowContext);
    const selectedElements = useStoreState((state) => state.selectedElements);
    // const storeActions = useStoreActions((a) => {
    //     console.log({ a });
    //     return a.setMaxZoom;
    // });
    const { invalid, label } = data;
    const [tempLabel, setTempLabel] = React.useState<string>(label);
    const [showLabel, setShowLabel] = React.useState<boolean>(true);

    const handleLabelClick = (e: any) => {
        e.stopPropagation();
        setShowLabel((l) => !l);
    };

    const handleLabelChange = (e) => {
        e.preventDefault();
        const updatedNodes = utility.elements.map((el) => {
            if (el.id === id) {
                el.data = {
                    ...el.data,
                    label: tempLabel,
                };
            }
            return el;
        });
        utility.dispatch({
            type: ACTION_TYPES.setElements,
            payload: updatedNodes,
        });
        setShowLabel(true);
    };

    const handleHelpIconClick = (e: any) => {
        e.stopPropagation();
        const chosenElementForTooltip = selectedElements?.find(
            (el) => el.id === id
        );
        utility.dispatch({
            type: ACTION_TYPES.activateTooltip,
            payload: {
                ...chosenElementForTooltip,
                content: `Required parameter missing (${label}), check the help dialog pane for more information`,
            },
        });
    };

    return (
        <>
            <Handle
                type="target"
                position={Position.Left}
                style={style}
                onConnect={onConnect}
            />
            <div className={`custom-node ${selected ? "active" : ""}`}>
                <div className="label-box">
                    {showLabel ? (
                        <div onClick={handleLabelClick}>
                            <p>{label}</p>
                        </div>
                    ) : (
                        <form onSubmit={handleLabelChange}>
                            <input
                                value={tempLabel}
                                onChange={(e) => setTempLabel(e.target.value)}
                                onBlur={handleLabelChange}
                            />
                        </form>
                    )}
                </div>
                <small>inp</small>
                <small>out</small>
                <div>
                    <Icon name="item-copy" large />
                </div>
                {invalid ? (
                    <div
                        className="custom-node__box"
                        onClick={handleHelpIconClick}
                    >
                        <Icon name="item-question" small color="#ff8303" />
                    </div>
                ) : null}
            </div>
            <Handle
                type="source"
                position={Position.Right}
                style={style}
                onConnect={onConnect}
            />
        </>
    );
};

export default React.memo(DoubleSlots);
