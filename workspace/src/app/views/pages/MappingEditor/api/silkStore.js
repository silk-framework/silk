import silkApi from "./silkRestApi";

/** Business logic layer over the DataIntegration REST API. */
const silkStore = {
    /**
     * Retrieves a transform task execution report.
     */
    getTransformExecutionReport: (projectId, taskId) => {
        return silkApi.activityResult(projectId, taskId, "ExecuteTransform").then(({ data }) => {
            return data;
        });
    },

    /**
     * Retrieves a linking task execution report.
     */
    getLinkingExecutionReport: (projectId, taskId) => {
        return silkApi.activityResult(projectId, taskId, "ExecuteLinking").then(({ data }) => {
            return data;
        });
    },

    /**
     * Retrieves execution reports for a single workflow node.
     */
    getWorkflowNodeExecutionReports: (projectId, taskId, nodeId) => {
        return silkApi.retrieveWorkflowNodeExecutionReports(projectId, taskId, nodeId).then(({ data }) => {
            return data;
        });
    },

    /**
     * Retrieves the current script code.
     */
    getScript: (projectId, scriptTaskId) => {
        return silkApi.getTask(projectId, scriptTaskId).then(({ data }) => {
            return data.data.parameters.script;
        });
    },

    /**
     * Retrieves auto completions for the script code at a specific code position.
     */
    getCompletions: (projectId, scriptTaskId, line, column) => {
        const requestJson = {
            line: line,
            column: column,
        };

        return silkApi.completions(projectId, scriptTaskId, requestJson);
    },

    /**
     * Retrieves a list of all available reports.
     */
    listExecutionReports: (projectId, taskId) => {
        return silkApi.listReports(projectId, taskId).then(({ data }) => {
            return data;
        });
    },

    /**
     * Retrieves a single report.
     */
    retrieveExecutionReport: (projectId, taskId, time) => {
        return silkApi.retrieveReport(projectId, taskId, time).then(({ data }) => {
            return data;
        });
    },

    /**
     * Saves the script code, executes the script in context of the workflow and returns the result tables.
     *
     * Result is a JSON representation of OperatorExecutionResult {"inputs": [{...}, {...}], "output": {...}}
     * "output" Can be undefined if there is no output
     * "inputs" can have 0 to many entries
     * Entries have the following form: {"attributes": ["prop1", "prop2", ...], "values": [[["prop1 val"], ["prop2 val"]], [[...]], ...]}
     * Each entry has the table headers/attributes and array-arrays of strings for each attribute, so the number of entries in "attributes" matches those of "values".
     *
     **/
    evaluateScript: (projectId, scriptTaskId, workflowId, workflowOperatorId, scriptCode) => {
        const patchJson = {
            data: {
                parameters: {
                    script: scriptCode,
                },
            },
        };
        const executeSparkOperatorActivity = "ExecuteSparkOperator";
        // Save script task with modified script, FIXME: Have separate 'Save' step, execute activity without the need of saving the script task
        return silkApi
            .patchTask(projectId, scriptTaskId, patchJson)
            .then(() => {
                return silkApi.executeTaskActivityBlocking(projectId, workflowId, executeSparkOperatorActivity, {
                    operator: workflowOperatorId,
                });
            })
            .then(() => {
                return silkApi.activityResult(projectId, workflowId, executeSparkOperatorActivity);
            });
    },
};

export default silkStore;
