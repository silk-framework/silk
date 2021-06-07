import React, { DragEvent } from "react";
import { FlowContext } from "./WorkflowEditor";

const dummyDataSet = [
    "movies",
    "Titanic Dataset",
    "Customer Reports",
    "Vacation Reports",
];

const dummyOperators = [
    "Read CSV",
    "Read Excel",
    "Write Excel",
    "Decision Tree",
];

const onDragStart = (event: DragEvent, nodeType: string, label: string) => {
    const data = JSON.stringify({ nodeType, label });
    event.dataTransfer.setData("application/reactflow", data);
    event.dataTransfer.effectAllowed = "move";
};

const SideBar = () => {
    const utility = React.useContext(FlowContext);

    return (
        <aside className="workflow-editor_sidebar">
            <div className="card">
                <h3>Operators</h3>
                {dummyOperators.map((operator) => (
                    <div
                        className="node"
                        key={operator}
                        onDragStart={(event: DragEvent) =>
                            onDragStart(event, "input", operator)
                        }
                        draggable
                    >
                        {operator}
                    </div>
                ))}
            </div>
            <div className="card">
                <h3>Dataset</h3>
                {dummyDataSet.map((dataset) => (
                    <div
                        className="node"
                        key={dataset}
                        onDragStart={(event: DragEvent) =>
                            onDragStart(event, "customNode", dataset)
                        }
                        draggable
                    >
                        {dataset}
                    </div>
                ))}
            </div>
            <div className="help-dialog card">
                <h3> Help Dialog</h3>
                <p>{utility.toolTip?.content}</p>
            </div>
        </aside>
    );
};

export default SideBar;
