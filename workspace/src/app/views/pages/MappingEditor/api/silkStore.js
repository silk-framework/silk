import silkApi from './silkRestApi';

/** Business logic layer over the DataIntegration REST API. */
const silkStore = {

    /**
     * Retrieves a transform task execution report.
     */
    getTransformExecutionReport: (projectId, taskId) => {
        return silkApi.activityResult(projectId, taskId, "ExecuteTransform")
            .then(({data}) => {
              return data;
            });
    },

    /**
     * Retrieves a linking task execution report.
     */
    getLinkingExecutionReport: (projectId, taskId) => {
        return silkApi.activityResult(projectId, taskId, "ExecuteLinking")
            .then(({data}) => {
                return data;
            });
    },

    /**
     * Retrieves execution reports for a single workflow node.
     */
    getWorkflowNodeExecutionReports: (projectId, taskId, nodeId) => {
        return silkApi.retrieveWorkflowNodeExecutionReports(projectId, taskId, nodeId)
            .then(({data}) => {
                return data;
            });
    },

    /**
     * Retrieves a list of all available reports.
     */
    listExecutionReports: (projectId, taskId) => {
        return silkApi.listReports(projectId, taskId)
            .then(({data}) => {
                return data;
            });
    },

    /**
     * Retrieves a single report.
     */
    retrieveExecutionReport: (projectId, taskId, time) => {
        return silkApi.retrieveReport(projectId, taskId, time)
            .then(({data}) => {
                return data;
            });
    }
};

export default silkStore;
