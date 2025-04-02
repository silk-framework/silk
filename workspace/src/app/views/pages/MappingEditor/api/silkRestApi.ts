import superagent from "@eccenca/superagent";
import Promise from "bluebird";
import { IUriPatternsResult, PropertyByDomainAutoCompletion, TargetPropertyAutoCompletion } from "./types";
import { CONTEXT_PATH } from "../../../../constants/path";
import { TaskContext } from "../../../shared/projectTaskTabView/projectTaskTabView.typing";
import { IPartialAutoCompleteResult } from "@eccenca/gui-elements/src/components/AutoSuggestion/AutoSuggestion";

const CONTENT_TYPE_JSON = "application/json";

export interface IHttpResponse<T> {
    status: number;
    data: T;
}

export type HttpResponsePromise<T> = Promise<IHttpResponse<T>>;

/**
 * API for the Silk REST API. This includes the pure REST calls returning the results as JSON without any
 * further business logic.
 */
const silkApi = {
    /** returns the JSON representation of a DI task */
    getTask: function (projectId, taskId) {
        const requestUrl = this.genericTaskEndpoint(projectId, taskId);

        const promise = superagent.get(requestUrl).accept(CONTENT_TYPE_JSON);

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
    patchTask: function (projectId, taskId, patchJson) {
        const requestUrl = this.genericTaskEndpoint(projectId, taskId);

        const promise = superagent.patch(requestUrl).set("Content-Type", CONTENT_TYPE_JSON).send(patchJson);
        return this.handleErrorCode(promise);
    },

    /** Configure a task activity. This should be done before starting the activity.
     *  @param config a JS object interpreted as Map containing the activity config parameters
     * */
    configureTaskActivity: function (projectId, taskId, activityId, config) {
        const requestUrl = this.taskActivityConfigEndpoint(projectId, taskId, activityId);

        const promise = superagent.post(requestUrl).type("form").send(config);

        return this.handleErrorCode(promise);
    },

    /** Executes an activity. Blocks until the activity finished executing. */
    executeTaskActivityBlocking: function (projectId, taskId, activityId, config) {
        const requestUrl = this.taskActivityExecuteBlockingEndpoint(projectId, taskId, activityId);

        const promise = superagent.post(requestUrl).type("form").send(config);

        return this.handleErrorCode(promise);
    },

    /**
     * Returns a promise of the activity result value as JSON.
     */
    activityResult: function (projectId, taskId, activityId) {
        const requestUrl = this.taskActivityValueEndpoint(projectId, taskId, activityId);

        const promise = superagent.get(requestUrl).accept(CONTENT_TYPE_JSON);

        return this.handleErrorCode(promise);
    },

    /**
     * Lists reports.
     */
    listReports: function (projectId, taskId) {
        const requestUrl = this.reportListEndpoint(projectId, taskId);

        const promise = superagent.get(requestUrl).accept(CONTENT_TYPE_JSON);

        return this.handleErrorCode(promise);
    },

    /**
     * Retrieves a report.
     */
    retrieveReport: function (projectId, taskId, time) {
        const requestUrl = this.reportEndpoint(projectId, taskId, time);

        const promise = superagent.get(requestUrl).accept(CONTENT_TYPE_JSON);

        return this.handleErrorCode(promise);
    },

    /**
     * Retrieves reports for a given workflow node.
     */
    retrieveWorkflowNodeExecutionReports: function (projectId, taskId, nodeId) {
        const requestUrl = this.workflowNodeExecutionReportsEndpoint(projectId, taskId, nodeId);

        const promise = superagent.get(requestUrl).accept(CONTENT_TYPE_JSON);

        return this.handleErrorCode(promise);
    },

    /** Retrieves information of the registered vocabularies of this transformation */
    retrieveTransformVocabularyInfos: function (projectId: string, transformTaskId: string): HttpResponsePromise<any> {
        const requestUrl = this.vocabularyInfoEndpoint(projectId, transformTaskId);

        const promise = superagent.get(requestUrl).accept(CONTENT_TYPE_JSON);

        return this.handleErrorCode(promise);
    },

    /**
     * Fetches infos about a target type or property URI from the vocabulary.
     * @param uri The target type or property URI.
     */
    retrieveTargetVocabularyTypeOrPropertyInfo: function (
        projectId: string,
        transformTaskId: string,
        uri: string
    ): HttpResponsePromise<any> {
        const requestUrl = this.targetVocabularyTypeOrPropertyInfoEndpoint(projectId, transformTaskId);

        const promise = superagent.get(requestUrl).accept(CONTENT_TYPE_JSON).query({
            uri,
        });

        return this.handleErrorCode(promise);
    },

    /** Retrieves target properties that are valid for the specific transform rule as target property. */
    retrieveTransformTargetProperties: function (
        projectId: string,
        taskId: string,
        ruleId: string,
        searchTerm?: string,
        maxResults: number = 30,
        vocabularies?: string[] | undefined,
        fullUris: boolean = true,
        taskContext?: TaskContext
    ): HttpResponsePromise<TargetPropertyAutoCompletion[]> {
        const requestUrl = this.transformTargetPropertyEndpoint(
            projectId,
            taskId,
            ruleId,
            searchTerm,
            maxResults,
            fullUris
        );

        const promise = superagent
            .post(requestUrl)
            .accept(CONTENT_TYPE_JSON)
            .set("Content-Type", CONTENT_TYPE_JSON)
            .send({
                vocabularies: vocabularies,
                taskContext,
            });

        return this.handleErrorCode(promise);
    },

    /** Retrieves target property auto-completions. */
    targetClassAutoCompletions: function (
        projectId: string,
        taskId: string,
        searchTerm: string | undefined,
        maxResults: number
    ): HttpResponsePromise<TargetPropertyAutoCompletion[]> {
        const requestUrl = this.transformTargetTypesEndpoint(
            projectId,
            taskId,
            "notUsedInBackend",
            searchTerm,
            maxResults
        );

        const promise = superagent.get(requestUrl).accept(CONTENT_TYPE_JSON);

        return this.handleErrorCode(promise);
    },

    propertiesByClass: function (
        projectId: string,
        transformTaskId: string,
        classUri: string,
        includeGeneralProperties?: boolean
    ): HttpResponsePromise<PropertyByDomainAutoCompletion[]> {
        const requestUrl = this.propertiesByTypeEndpoint(projectId, transformTaskId);

        return this.handleErrorCode(
            superagent.get(requestUrl).accept(CONTENT_TYPE_JSON).query({
                classUri,
                includeGeneralProperties,
            })
        );
    },

    /**
     * Requests auto-completion suggestions for the script task code.
     */
    completions: function (projectId, taskId, requestJson) {
        const requestUrl = this.completionEndpoint(projectId, taskId);

        const promise = superagent
            .post(requestUrl)
            .accept(CONTENT_TYPE_JSON)
            .set("Content-Type", CONTENT_TYPE_JSON)
            .send(requestJson);

        return this.handleErrorCode(promise);
    },

    /** Returns information relevant for initializing the UI. */
    initFrontendInfo: function () {
        const requestUrl = this.initFrontendEndpoint();

        return this.handleErrorCode(superagent.get(requestUrl).accept(CONTENT_TYPE_JSON));
    },

    /** Returns all known URI patterns for the given type URIs. */
    uriPatternsByTypes: function (projectId: string, typeUris: string[]): HttpResponsePromise<IUriPatternsResult> {
        const requestUrl = this.uriPatternsByTypesEndpoint();

        return this.handleErrorCode(
            superagent
                .post(requestUrl)
                .accept(CONTENT_TYPE_JSON)
                .set("Content-Type", CONTENT_TYPE_JSON)
                .send({ projectId, targetClassUris: typeUris })
        );
    },

    /**
     * Handles the HTTP status. Calls reject for error codes != 2xx
     * @param superAgentPromise The promise from the superagent API
     * @param bodyHandler       function to convert the body or set the data field with a constant value.
     * @returns Promise
     */
    handleErrorCode: <T>(superAgentPromise): HttpResponsePromise<T> => {
        return new Promise((resolve, reject) => {
            superAgentPromise
                .then(({ status, body }) => {
                    if (status < 200 || status >= 300) {
                        return reject({ status, data: body });
                    }
                    return resolve({ status, data: body });
                })
                .catch((err) => reject(err));
        });
    },

    // Root URL of the (new) REST API
    apiBase: function () {
        return `${CONTEXT_PATH}/api`;
    },

    // Root URL of the (new) workspace REST API
    workspaceApi: function () {
        return `${this.apiBase()}/workspace`;
    },

    // Endpoint the returns basis information for the (new) frontend UI to initialize.
    initFrontendEndpoint: function () {
        return `${this.workspaceApi()}/initFrontend`;
    },

    genericTaskEndpoint: function (projectId, taskId) {
        return `${CONTEXT_PATH}/workspace/projects/${projectId}/tasks/${taskId}`;
    },

    taskActivityExecuteBlockingEndpoint: function (projectId, taskId, activityId) {
        return `${CONTEXT_PATH}/workspace/projects/${projectId}/tasks/${taskId}/activities/${activityId}/startBlocking`;
    },

    taskActivityValueEndpoint: function (projectId, taskId, activityId) {
        return `${CONTEXT_PATH}/workspace/projects/${projectId}/tasks/${taskId}/activities/${activityId}/value`;
    },

    /** Endpoint for configuring an activity or getting the current configuration.
     **/
    taskActivityConfigEndpoint: function (projectId, taskId, activityId) {
        return `${CONTEXT_PATH}/workspace/projects/${projectId}/tasks/${taskId}/activities/${activityId}/config`;
    },

    completionEndpoint: function (projectId, taskId) {
        return `${CONTEXT_PATH}/scripts/projects/${projectId}/tasks/${taskId}/completions`;
    },

    reportListEndpoint: function (projectId, taskId) {
        return `${CONTEXT_PATH}/api/workspace/reports/list?projectId=${projectId}&taskId=${taskId}`;
    },

    reportEndpoint: function (projectId, taskId, time) {
        return `${CONTEXT_PATH}/api/workspace/reports/report?projectId=${projectId}&taskId=${taskId}&time=${time}`;
    },

    workflowNodeExecutionReportsEndpoint: function (projectId, taskId, nodeId) {
        return `${CONTEXT_PATH}/api/workspace/reports/currentReport/nodeReports?projectId=${projectId}&taskId=${taskId}&nodeId=${nodeId}`;
    },

    vocabularyInfoEndpoint: function (projectId: string, transformTaskId: string) {
        return `${CONTEXT_PATH}/transform/tasks/${projectId}/${transformTaskId}/targetVocabulary/vocabularies`;
    },

    targetVocabularyTypeOrPropertyInfoEndpoint: function (projectId: string, transformTaskId: string) {
        return `${CONTEXT_PATH}/transform/tasks/${projectId}/${transformTaskId}/targetVocabulary/typeOrProperty`;
    },

    uriPatternsByTypesEndpoint: function () {
        return `${CONTEXT_PATH}/api/workspace/uriPatterns`;
    },

    transformTargetPropertyEndpoint: function (
        projectId: string,
        transformTaskId: string,
        ruleId: string,
        searchTerm: string | undefined,
        maxResults: number,
        fullUris: boolean
    ): string {
        const encodedSearchTerm = searchTerm ? encodeURIComponent(searchTerm) : "";
        return `${CONTEXT_PATH}/transform/tasks/${projectId}/${transformTaskId}/rule/${ruleId}/completions/targetProperties?term=${encodedSearchTerm}&maxResults=${maxResults}&fullUris=${fullUris}`;
    },

    transformTargetTypesEndpoint: function (
        projectId: string,
        transformTaskId: string,
        ruleId: string,
        searchTerm: string | undefined,
        maxResults: number
    ): string {
        const encodedSearchTerm = searchTerm ? encodeURIComponent(searchTerm) : "";
        return `${CONTEXT_PATH}/transform/tasks/${projectId}/${transformTaskId}/rule/${ruleId}/completions/targetTypes?term=${encodedSearchTerm}&maxResults=${maxResults}`;
    },

    propertiesByTypeEndpoint: function (projectId: string, transformTaskId: string): string {
        return `${CONTEXT_PATH}/transform/tasks/${projectId}/${transformTaskId}/targetVocabulary/propertiesByClass`;
    },

    getSuggestionsForAutoCompletion: function (
        projectId: string,
        transformTaskId: string,
        ruleId: string,
        inputString: string,
        cursorPosition: number,
        isObjectPath: boolean,
        taskContext?: TaskContext,
        baseSourcePath?: string,
        oneHopOnly?: boolean,
        ignorePathOperatorCompletions?: boolean
    ): HttpResponsePromise<IPartialAutoCompleteResult> {
        const requestUrl = `${CONTEXT_PATH}/transform/tasks/${projectId}/${transformTaskId}/rule/${ruleId}/completions/partialSourcePaths`;
        const requestBody: any = {
            inputString,
            cursorPosition,
            maxSuggestions: 50,
            isObjectPath,
            taskContext,
            baseSourcePath,
        };
        if (oneHopOnly) {
            requestBody.oneHopOnly = true;
        }
        if (ignorePathOperatorCompletions) {
            requestBody.ignorePathOperatorCompletions = true;
        }
        const promise = superagent.post(requestUrl).set("Content-Type", CONTENT_TYPE_JSON).send(requestBody);
        return this.handleErrorCode(promise);
    },

    getUriTemplateSuggestionsForAutoCompletion: function (
        projectId: string,
        transformTaskId: string,
        ruleId: string,
        inputString: string,
        cursorPosition: number,
        objectContextPath?: string
    ): HttpResponsePromise<any> {
        const requestUrl = `${CONTEXT_PATH}/transform/tasks/${projectId}/${transformTaskId}/rule/${ruleId}/completions/uriPattern`;
        const promise = superagent
            .post(requestUrl)
            .set("Content-Type", CONTENT_TYPE_JSON)
            .send({ inputString, cursorPosition, maxSuggestions: 50, objectPath: objectContextPath });
        return this.handleErrorCode(promise);
    },

    validatePathExpression: function (projectId: string, pathExpression: string) {
        const requestUrl = `${CONTEXT_PATH}/api/workspace/validation/sourcePath/${projectId}`;
        const promise = superagent.post(requestUrl).set("Content-Type", CONTENT_TYPE_JSON).send({ pathExpression });
        return this.handleErrorCode(promise);
    },

    validateUriPattern: function (projectId: string, uriPattern: string) {
        const requestUrl = `${CONTEXT_PATH}/api/workspace/validation/uriPattern/${projectId}`;
        const promise = superagent.post(requestUrl).set("Content-Type", CONTENT_TYPE_JSON).send({ uriPattern });
        return this.handleErrorCode(promise);
    },

    /** Reorder the (child) mapping rules of a root/object mapping. */
    reorderRules: function (projectId: string, transformTaskId: string, ruleId: string, childrenRules: any) {
        const requestUrl = `${CONTEXT_PATH}/transform/tasks/${projectId}/${transformTaskId}/rule/${ruleId}/rules/reorder`;
        const promise = superagent.post(requestUrl).set("Content-Type", CONTENT_TYPE_JSON).send(childrenRules);
        return this.handleErrorCode(promise);
    },
};

export default silkApi;
