import "react-app-polyfill/ie11";
import "react-app-polyfill/stable";

import React from 'react';
import ReactDom from 'react-dom';

import HierarchicalMappingComponent from './HierarchicalMapping/HierarchicalMapping';
import ExecutionReport from "./ExecutionReport/ExecutionReport";
import TransformExecutionReport from "./ExecutionReport/TransformExecutionReport";
import WorkflowExecutionReport from "./ExecutionReport/WorkflowExecutionReport";
import EvaluateMapping from "./HierarchicalMapping/EvaluateMapping";
import WorkflowReportManager from "./ExecutionReport/WorkflowReportManager";

// eslint-disable-next-line
import SilkStore from './SilkStore/silkStore';
import WorkflowNodeExecutionReport from "./ExecutionReport/WorkflowNodeExecutionReport";

require('./style/style.scss');

window.silkReactComponents = {
    hierarchicalMapping: (containerId, apiSettings) => {
        ReactDom.render(
            <HierarchicalMappingComponent {...apiSettings} />,
            document.getElementById(containerId)
        );
    },
    evaluateMapping: (containerId, apiSettings) => {
        ReactDom.render(
            <EvaluateMapping {...apiSettings} />,
            document.getElementById(containerId)
        );
    },
    executionReport: (containerId, apiSettings) => {
        ReactDom.render(
            <ExecutionReport {...apiSettings} />,
            document.getElementById(containerId)
        );
    },
    transformExecutionReport: (containerId, apiSettings) => {
        ReactDom.render(
            <TransformExecutionReport {...apiSettings} />,
            document.getElementById(containerId)
        );
    },
    workflowExecutionReport: (containerId, apiSettings) => {
        ReactDom.render(
            <WorkflowExecutionReport {...apiSettings} />,
            document.getElementById(containerId)
        );
    },
    workflowNodeExecutionReport: (containerId, apiSettings) => {
        ReactDom.render(
            <WorkflowNodeExecutionReport {...apiSettings} />,
            document.getElementById(containerId)
        );
    },
    workflowReportManager: (containerId, apiSettings) => {
        ReactDom.render(
            <WorkflowReportManager {...apiSettings} />,
            document.getElementById(containerId)
        );
    }
};

