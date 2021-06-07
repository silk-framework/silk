import React from "react";
import { ACTION_TYPES, FlowContext } from "../WorkflowEditor";

const FlowTooltip = () => {
    const utility = React.useContext(FlowContext);
    if (!utility.toolTip) return null;
    const { x, y } = utility.toolTip.position;
    const positionStyles = {
        left: `${x + 40}px`,
        top: `${y + 80}px`,
    };

    const respondToGotItClick = () => {
        utility.dispatch({
            type: ACTION_TYPES.activateTooltip,
            payload: null,
        });
    };

    return (
        <div className="flow__tooltip" style={positionStyles}>
            <p>{utility.toolTip.content}</p>
            <button onClick={respondToGotItClick}>Got it!</button>
        </div>
    );
};

export default FlowTooltip;
