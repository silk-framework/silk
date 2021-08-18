import superagent from '@eccenca/superagent';
import Promise from 'bluebird';

const CONTENT_TYPE_JSON = 'application/json';

export interface IHttpResponse {
    status: number
    data: any
}

export type HttpResponsePromise = Promise<IHttpResponse>

/**
 * API for the DataIntegration REST API. This includes the pure REST calls returning the results as JSON without any
 * further business logic.
 */
const silkApi = {

    /** returns the JSON representation of a DI task */
    getTask: function(baseUrl, projectId, taskId) {
        const requestUrl = this.genericTaskEndpoint(baseUrl, projectId, taskId);

        const promise = superagent
            .get(requestUrl)
            .accept(CONTENT_TYPE_JSON);

        return this.handleErrorCode(promise);
    },

    /** Patches the DI task with the provided path JSON. The JSON is merged with the current version of the task.
     *  The merge functions as follows:
     *  - For all undefined properties in the patch object the old property values are taken
     *  - For all defined properties it depends on the type of the value:
     *    - If the new value is an object and the old value is also an object both objects are also deep merged
     *    - If the new value is not an object it will overwrite the old value
     *  - It's not possible to remove a property, but only to set it to null. TODO: Clarify with Robert if there is a way to remove a property
     **/
    patchTask: function( baseUrl, projectId, taskId, patchJson) {
        const requestUrl = this.genericTaskEndpoint(baseUrl, projectId, taskId);

        const promise = superagent
            .patch(requestUrl)
            .set('Content-Type', CONTENT_TYPE_JSON)
            .send(patchJson);
        return this.handleErrorCode(promise);
    },

    /** Configure a task activity. This should be done before starting the activity.
     *  @param config a JS object interpreted as Map containing the activity config parameters
     * */
    configureTaskActivity: function(baseUrl, projectId, taskId, activityId, config) {
        const requestUrl = this.taskActivityConfigEndpoint(baseUrl, projectId, taskId, activityId);

        const promise = superagent
            .post(requestUrl)
            .type('form')
            .send(config);

        return this.handleErrorCode(promise);
    },

    /** Executes an activity. Blocks until the activity finished executing. */
    executeTaskActivityBlocking: function(baseUrl, projectId, taskId, activityId, config) {
        const requestUrl = this.taskActivityExecuteBlockingEndpoint(baseUrl, projectId, taskId, activityId);

        const promise = superagent
            .post(requestUrl)
            .type('form')
            .send(config);

        return this.handleErrorCode(promise);
    },

    /**
     * Returns a promise of the activity result value as JSON.
     */
    activityResult: function(baseUrl, projectId, taskId, activityId) {
        const requestUrl = this.taskActivityValueEndpoint(baseUrl, projectId, taskId, activityId);

        const promise = superagent
            .get(requestUrl)
            .accept(CONTENT_TYPE_JSON);

        return this.handleErrorCode(promise);
    },

    /**
     * Lists reports.
     */
    listReports: function(baseUrl, projectId, taskId) {
        const requestUrl = this.reportListEndpoint(baseUrl, projectId, taskId);

        const promise = superagent
            .get(requestUrl)
            .accept(CONTENT_TYPE_JSON);

        return this.handleErrorCode(promise);
    },

    /**
     * Retrieves a report.
     */
    retrieveReport: function(baseUrl, projectId, taskId, time) {
        const requestUrl = this.reportEndpoint(baseUrl, projectId, taskId, time);

        const promise = superagent
            .get(requestUrl)
            .accept(CONTENT_TYPE_JSON);

        return this.handleErrorCode(promise);
    },

    /** Retrieves information of the registered vocabularies of this transformation */
    retrieveTransformVocabularyInfos: function(baseUrl: string, projectId: string, transformTaskId: string): HttpResponsePromise {
        const requestUrl = this.vocabularyInfoEndpoint(baseUrl, projectId, transformTaskId)

        const promise = superagent
            .get(requestUrl)
            .accept(CONTENT_TYPE_JSON);

        return this.handleErrorCode(promise);
    },

    /** Retrieves target properties that are valid for the specific transform rule as target property. */
    retrieveTransformTargetProperties: function(baseUrl: string, projectId: string, taskId: string, ruleId: string,
                                                searchTerm?: string, maxResults: number = 30, vocabularies?: string[],
                                                fullUris: boolean = true): HttpResponsePromise {
        const requestUrl = this.transformTargetPropertyEndpoint(baseUrl, projectId, taskId, ruleId, searchTerm, maxResults, fullUris);

        const promise = superagent
            .post(requestUrl)
            .accept(CONTENT_TYPE_JSON)
            .set('Content-Type', CONTENT_TYPE_JSON)
            .send({vocabularies: vocabularies})

        return this.handleErrorCode(promise);
    },

    /**
     * Requests auto-completion suggestions for the script task code.
     */
    completions: function(baseUrl, projectId, taskId, requestJson) {
        const requestUrl = this.completionEndpoint(baseUrl, projectId, taskId);

        const promise = superagent
            .post(requestUrl)
            .accept(CONTENT_TYPE_JSON)
            .set('Content-Type', CONTENT_TYPE_JSON)
            .send(requestJson);

        return this.handleErrorCode(promise);
    },

    /** Returns information relevant for initializing the UI. */
    initFrontendInfo: function(baseUrl) {
        const requestUrl = this.initFrontendEndpoint(baseUrl)

        return this.handleErrorCode(
            superagent
                .get(requestUrl)
                .accept(CONTENT_TYPE_JSON)
        )
    },

    /**
     * Handles the HTTP status. Calls reject for error codes != 2xx
     * @param superAgentPromise The promise from the superagent API
     * @param bodyHandler       function to convert the body or set the data field with a constant value.
     * @returns Promise
     */
    handleErrorCode: (superAgentPromise): HttpResponsePromise => {
        return new Promise((resolve, reject) => {
            superAgentPromise
                .then(({ status, body }) => {
                    if (status < 200 || status >= 300) {
                        return reject({ status, data: body });
                    }
                    return resolve({ status, data: body });
                })
                .catch(err => reject(err))
        });
    },

    // Root URL of the (new) REST API
    apiBase: function(baseUrl) {
        return `${baseUrl}/api`
    },

    // Root URL of the (new) workspace REST API
    workspaceApi: function(baseUrl) {
        return `${this.apiBase(baseUrl)}/workspace`
    },

    // Endpoint the returns basis information for the (new) frontend UI to initialize.
    initFrontendEndpoint: function (baseUrl) {
        return `${this.workspaceApi(baseUrl)}/initFrontend`
    },

    genericTaskEndpoint: function(baseUrl, projectId, taskId) {
        return `${baseUrl}/workspace/projects/${projectId}/tasks/${taskId}`;
    },

    taskActivityExecuteBlockingEndpoint: function(baseUrl, projectId, taskId, activityId) {
        return `${baseUrl}/workspace/projects/${projectId}/tasks/${taskId}/activities/${activityId}/startBlocking`;
    },

    taskActivityValueEndpoint: function(baseUrl, projectId, taskId, activityId) {
        return `${baseUrl}/workspace/projects/${projectId}/tasks/${taskId}/activities/${activityId}/value`;
    },

    /** Endpoint for configuring an activity or getting the current configuration.
     **/
    taskActivityConfigEndpoint: function(baseUrl, projectId, taskId, activityId) {
        return `${baseUrl}/workspace/projects/${projectId}/tasks/${taskId}/activities/${activityId}/config`;
    },

    completionEndpoint: function(baseUrl, projectId, taskId) {
        return `${baseUrl}/scripts/projects/${projectId}/tasks/${taskId}/completions`;
    },

    reportListEndpoint: function(baseUrl, projectId, taskId) {
        return `${baseUrl}/api/workspace/reports/list?projectId=${projectId}&taskId=${taskId}`;
    },

    reportEndpoint: function(baseUrl, projectId, taskId, time) {
        return `${baseUrl}/api/workspace/reports/report?projectId=${projectId}&taskId=${taskId}&time=${time}`;
    },

    vocabularyInfoEndpoint: function(baseUrl: string, projectId: string, transformTaskId: string) {
        return `${baseUrl}/transform/tasks/${projectId}/${transformTaskId}/targetVocabulary/vocabularies`;
    },

    transformTargetPropertyEndpoint: function(baseUrl: string, projectId: string, transformTaskId: string, ruleId: string,
                                              searchTerm: string, maxResults: number, fullUris: boolean): string {
        const encodedSearchTerm = searchTerm ? encodeURIComponent(searchTerm) : ""
        return `${baseUrl}/transform/tasks/${projectId}/${transformTaskId}/rule/${ruleId}/completions/targetProperties?term=${encodedSearchTerm}&maxResults=${maxResults}&fullUris=${fullUris}`
    },

    getSuggestionsForAutoCompletion: function(baseUrl: string, projectId:string, transformTaskId:string, ruleId:string, inputString:string, cursorPosition: number): HttpResponsePromise {
        const requestUrl = `${baseUrl}/transform/tasks/${projectId}/${transformTaskId}/rule/${ruleId}/completions/partialSourcePaths`;
        const promise = superagent
            .post(requestUrl)
            .set("Content-Type", CONTENT_TYPE_JSON)
            .send({ inputString, cursorPosition, maxSuggestions: 50 });
        return this.handleErrorCode(promise)
    },

    getUriTemplateSuggestionsForAutoCompletion: function(baseUrl: string, projectId:string, transformTaskId:string, ruleId:string, inputString:string, cursorPosition: number): HttpResponsePromise {
        const requestUrl = `${baseUrl}/transform/tasks/${projectId}/${transformTaskId}/rule/${ruleId}/completions/uriPattern`;
        const promise = superagent
            .post(requestUrl)
            .set("Content-Type", CONTENT_TYPE_JSON)
            .send({ inputString, cursorPosition, maxSuggestions: 50 });
        return this.handleErrorCode(promise)
    },

    validatePathExpression: function(baseUrl:string, projectId:string, pathExpression: string) {
        const requestUrl = `${baseUrl}/api/workspace/validation/sourcePath/${projectId}`;
        const promise = superagent
            .post(requestUrl)
            .set("Content-Type", CONTENT_TYPE_JSON)
            .send({ pathExpression });
        return this.handleErrorCode(promise);
    },

    validateUriPattern: function(baseUrl:string, projectId:string, uriPattern: string) {
        const requestUrl = `${baseUrl}/api/workspace/validation/uriPattern/${projectId}`;
        const promise = superagent
            .post(requestUrl)
            .set("Content-Type", CONTENT_TYPE_JSON)
            .send({ uriPattern });
        return this.handleErrorCode(promise);
    }

};

export default silkApi
