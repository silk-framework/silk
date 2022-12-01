import "react-app-polyfill/ie11";
import "react-app-polyfill/stable";

import React from "react";
import ReactDom from "react-dom";

import ExecutionReport from "./ExecutionReport/ExecutionReport";
import LinkingExecutionReport from "./ExecutionReport/LinkingExecutionReport";
import TransformExecutionReport from "./ExecutionReport/TransformExecutionReport";
import WorkflowExecutionReport from "./ExecutionReport/WorkflowExecutionReport";
import WorkflowNodeExecutionReport from "./ExecutionReport/WorkflowNodeExecutionReport";
import WorkflowReportManager from "./ExecutionReport/WorkflowReportManager";
import EvaluateMapping from "./HierarchicalMapping/EvaluateMapping";
import HierarchicalMappingComponent from "./HierarchicalMapping/HierarchicalMapping";
// eslint-disable-next-line
import SilkStore from './SilkStore/silkStore';

const silkReactComponents = {
    hierarchicalMapping: (containerId, apiSettings) => {
        ReactDom.render(<HierarchicalMappingComponent {...apiSettings} />, document.getElementById(containerId));
    },
    evaluateMapping: (containerId, apiSettings) => {
        ReactDom.render(<EvaluateMapping {...apiSettings} />, document.getElementById(containerId));
    },
    executionReport: (containerId, apiSettings) => {
        ReactDom.render(<ExecutionReport {...apiSettings} />, document.getElementById(containerId));
    },
    transformExecutionReport: (containerId, apiSettings) => {
        ReactDom.render(<TransformExecutionReport {...apiSettings} />, document.getElementById(containerId));
    },
    linkingExecutionReport: (containerId, apiSettings) => {
        ReactDom.render(<LinkingExecutionReport {...apiSettings} />, document.getElementById(containerId));
    },
    workflowExecutionReport: (containerId, apiSettings) => {
        ReactDom.render(<WorkflowExecutionReport {...apiSettings} />, document.getElementById(containerId));
    },
    workflowNodeExecutionReport: (containerId, apiSettings) => {
        ReactDom.render(<WorkflowNodeExecutionReport {...apiSettings} />, document.getElementById(containerId));
    },
    workflowReportManager: (containerId, apiSettings) => {
        ReactDom.render(<WorkflowReportManager {...apiSettings} />, document.getElementById(containerId));
    },
};

window.silkReactComponents = silkReactComponents;

export default silkReactComponents;
