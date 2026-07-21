import React from "react";
import "@testing-library/jest-dom";
import mockAxios from "../../../../__mocks__/axios";
import {
    apiUrl,
    byTestId,
    checkRequestMade,
    findAllDOMElements,
    findElement,
    legacyApiUrl,
    mockedAxiosResponse,
    renderWrapper,
    setUseParams,
    workspacePath,
} from "../../../TestHelper";
import { createBrowserHistory } from "history";
import Task from "../../../../../src/app/views/pages/Task";
import {
    ITaskParameter,
    ParameterDescriptionGenerator,
    requestArtefactPropertiesTestResponse,
    requestTaskDataTestResponse,
} from "../../../requests/sharedResponseStubs";
import { RenderResult, waitFor } from "@testing-library/react";
import { IArtefactItemProperty } from "../../../../../src/app/store/ducks/common/typings";
import { IMetadata } from "@ducks/shared/typings";

describe("Task page", () => {
    afterEach(() => {
        mockAxios.reset();
    });

    const projectId = "cmem";
    const taskId = "taskId";
    const taskLabel = "A task";
    const taskDescription = "This is a task";
    const createdBy = "unknown";
    const lastModifiedBy = "unknown";
    const pluginId = "testPlugin";
    const pluginLabel = "Test Plugin";
    const taskDataUrl = legacyApiUrl(`/workspace/projects/${projectId}/tasks/${taskId}`);
    const taskMetaDataExpandedURL = legacyApiUrl(`/workspace/projects/${projectId}/tasks/${taskId}/metadataExpanded`);
    const pluginUrl = apiUrl(`/core/plugins/${pluginId}`);

    beforeAll(() => {
        setUseParams(projectId, taskId);
    });

    let taskPageWrapper: RenderResult;
    beforeEach(() => {
        const history = createBrowserHistory();
        history.location.pathname = workspacePath(`/projects/${projectId}/task/${taskId}`);

        taskPageWrapper = renderWrapper(<Task />, history);
    });

    it("should request meta data and task config", async () => {
        checkRequestMade(taskMetaDataExpandedURL);
        checkRequestMade(apiUrl(`/workspace/projects/${projectId}/tasks/${taskId}/relatedItems`));
        checkRequestMade(taskDataUrl, "GET", { withLabels: true });
        mockAxios.mockResponseFor(
            taskDataUrl,
            mockedAxiosResponse({ data: requestTaskDataTestResponse({ pluginId: pluginId }) }),
        );
        await waitFor(() => checkRequestMade(pluginUrl));
        // The task data must only be requested once (by the TaskConfig widget).
        expect(mockAxios.getReqByUrl(taskDataUrl)).toBeUndefined();
    });

    it("should display the task config with labels", async () => {
        const parameterGenerator = new ParameterDescriptionGenerator();
        const testParameterDescriptions: Record<string, IArtefactItemProperty> = {};
        const params = [
            ["param1", "First parameter", "first value"],
            ["param2", "Second parameter", "second value", "second value label"],
        ];
        params.forEach(([paramId, paramLabel]) => {
            testParameterDescriptions[paramId] = parameterGenerator.withValues({ title: paramLabel }).parameter();
        });
        const taskParams: ITaskParameter[] = params.map(([paramId, paramLabel, paramValue, paramValueLabel]) => {
            return { id: paramId, value: paramValue, label: paramValueLabel };
        });
        // The following two parameters should not show up in the result
        testParameterDescriptions["notShown1"] = parameterGenerator
            .withValues({ title: "Not shown 1", visibleInDialog: false })
            .parameter();
        testParameterDescriptions["notShown2"] = parameterGenerator
            .withValues({ title: "Not shown 2", advanced: true })
            .parameter();
        taskParams.push({ id: "notShown1", value: "not shown" });
        taskParams.push({ id: "notShown2", value: "not shown" });
        // Get task data. It is requested exactly once, by the TaskConfig widget, which shares
        // the plugin details with the page via its pluginDataCallback.
        mockAxios.mockResponseFor(
            taskDataUrl,
            mockedAxiosResponse({ data: requestTaskDataTestResponse({ pluginId: pluginId, parameters: taskParams }) }),
        );
        // Get plugin description. It is requested exactly once.
        await waitFor(() => checkRequestMade(pluginUrl));
        mockAxios.mockResponseFor(
            pluginUrl,
            mockedAxiosResponse({
                data: requestArtefactPropertiesTestResponse({
                    pluginLabel: pluginLabel,
                    properties: testParameterDescriptions,
                }),
            }),
        );
        // Check widget title
        await waitFor(() => {
            const taskConfig = findElement(taskPageWrapper, byTestId("taskConfigWidget"));
            expect(findElement(taskConfig, "header h2").textContent).toContain(pluginLabel);
        });
        // No further requests must have been made to either endpoint.
        expect(mockAxios.getReqByUrl(taskDataUrl)).toBeUndefined();
        expect(mockAxios.getReqByUrl(pluginUrl)).toBeUndefined();

        const taskConfig = findElement(taskPageWrapper, byTestId("taskConfigWidget"));
        const propertyLabels = findAllDOMElements(taskConfig, ".eccgui-card__content .eccgui-label").map(
            (elem) => elem.textContent,
        );
        expect(propertyLabels).toStrictEqual(params.map(([paramId, paramLabel]) => paramLabel));
        const propertyValues = findAllDOMElements(taskConfig, ".eccgui-card__content .eccgui-propertyvalue__value").map(
            (elem) => elem.textContent,
        );

        expect(propertyValues).toStrictEqual(
            params.map(([pluginId, pluginLabel, value, label]) => (label ? label : value)),
        );
    });

    it("should display meta data of the task", async () => {
        const taskMetaData: IMetadata = {
            label: taskLabel,
            description: taskDescription,
            modified: new Date(),
            created: new Date(),
        };
        mockAxios.mockResponseFor(taskMetaDataExpandedURL, mockedAxiosResponse({ data: taskMetaData }));
        await waitFor(() => {
            const metaData = findElement(taskPageWrapper, byTestId("metaDataWidget"));
            // The summary shows the label and description as property rows plus a separate audit info line.
            const propertyValues = findAllDOMElements(metaData, ".eccgui-propertyvalue__value").map(
                (elem) => elem.textContent,
            );
            expect(propertyValues).toStrictEqual([taskLabel, taskDescription]);
            const auditInfo = findElement(metaData, byTestId("metadata-audit-info"));
            expect(auditInfo.textContent).toContain("Created < 1 minute ago by unknown user.");
            expect(auditInfo.textContent).toContain("Last modified < 1 minute ago by unknown user.");
        });
    });
});
