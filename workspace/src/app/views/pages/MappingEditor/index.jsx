import "react-app-polyfill/ie11";
import "react-app-polyfill/stable";

import React from "react";
import { createRoot } from "react-dom/client";

import HierarchicalMappingComponent from "./HierarchicalMapping/HierarchicalMapping";
import ExecutionReport from "./ExecutionReport/ExecutionReport";
import TransformExecutionReport from "./ExecutionReport/TransformExecutionReport";
import WorkflowExecutionReport from "./ExecutionReport/WorkflowExecutionReport";
import WorkflowReportManager from "./ExecutionReport/WorkflowReportManager";

// eslint-disable-next-line
import SilkStore from "./SilkStore/silkStore";
import WorkflowNodeExecutionReport from "./ExecutionReport/WorkflowNodeExecutionReport";
import LinkingExecutionReport from "./ExecutionReport/LinkingExecutionReport";

const render = (elementId, reactNode) => {
    const root = createRoot(document.getElementById(elementId));
    root.render(reactNode);
};

const silkReactComponents = {
    hierarchicalMapping: (containerId, apiSettings) => {
        render(containerId, <HierarchicalMappingComponent {...apiSettings} />);
    },
    executionReport: (containerId, apiSettings) => {
        render(containerId, <ExecutionReport {...apiSettings} />);
    },
    transformExecutionReport: (containerId, apiSettings) => {
        render(containerId, <TransformExecutionReport {...apiSettings} />);
    },
    linkingExecutionReport: (containerId, apiSettings) => {
        render(containerId, <LinkingExecutionReport {...apiSettings} />);
    },
    workflowExecutionReport: (containerId, apiSettings) => {
        render(containerId, <WorkflowExecutionReport {...apiSettings} />);
    },
    workflowNodeExecutionReport: (containerId, apiSettings) => {
        render(containerId, <WorkflowNodeExecutionReport {...apiSettings} />);
    },
    workflowReportManager: (containerId, apiSettings) => {
        render(containerId, <WorkflowReportManager {...apiSettings} />);
    },
};

window.silkReactComponents = silkReactComponents;

export default silkReactComponents;
