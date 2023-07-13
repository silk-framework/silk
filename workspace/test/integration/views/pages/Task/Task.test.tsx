import React from "react";
import mockAxios from "../../../../__mocks__/axios";
import {
    apiUrl,
    byTestId,
    checkRequestMade,
    findAll,
    findSingleElement,
    legacyApiUrl,
    mockedAxiosResponse,
    setUseParams,
    testWrapper,
    withMount,
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
import { waitFor } from "@testing-library/react";
import { ReactWrapper } from "enzyme";
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

    let taskPageWrapper: ReactWrapper<any, any> = null;
    beforeEach(() => {
        const history = createBrowserHistory();
        history.location.pathname = workspacePath(`/projects/${projectId}/task/${taskId}`);

        taskPageWrapper = withMount(testWrapper(<Task />, history));
    });

    it("should request meta data, related items and task config", async () => {
        checkRequestMade(taskMetaDataExpandedURL);
        checkRequestMade(apiUrl(`/workspace/projects/${projectId}/tasks/${taskId}/relatedItems`));
        checkRequestMade(taskDataUrl, "GET", { withLabels: true });
        mockAxios.mockResponseFor(
            taskDataUrl,
            mockedAxiosResponse({ data: requestTaskDataTestResponse({ pluginId: pluginId }) })
        );
        await waitFor(() => checkRequestMade(pluginUrl));
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
        // Get task data
        mockAxios.mockResponseFor(
            taskDataUrl,
            mockedAxiosResponse({ data: requestTaskDataTestResponse({ pluginId: pluginId, parameters: taskParams }) })
        );
        await waitFor(() => checkRequestMade(pluginUrl));
        // Get plugin description
        mockAxios.mockResponseFor(
            pluginUrl,
            mockedAxiosResponse({
                data: requestArtefactPropertiesTestResponse({
                    pluginLabel: pluginLabel,
                    properties: testParameterDescriptions,
                }),
            })
        );
        // Check widget title
        await waitFor(() => {
            const taskConfig = findSingleElement(taskPageWrapper, byTestId("taskConfigWidget"));
            expect(findSingleElement(taskConfig, "header h2").text()).toContain(pluginLabel);
        });

        const taskConfig = findSingleElement(taskPageWrapper, byTestId("taskConfigWidget"));
        const propertyLabels = findAll(taskConfig, ".eccgui-card__content .eccgui-label").map((elem) => elem.text());
        expect(propertyLabels).toStrictEqual(params.map(([paramId, paramLabel]) => paramLabel));
        const propertyValues = findAll(taskConfig, ".eccgui-card__content .eccgui-propertyvalue__value").map((elem) =>
            elem.text()
        );

        expect(propertyValues).toStrictEqual(
            params.map(([pluginId, pluginLabel, value, label]) => (label ? label : value))
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
            const metaData = findSingleElement(taskPageWrapper, byTestId("metaDataWidget"));
            expect(findAll(metaData, ".eccgui-propertyvalue__value").map((elem) => elem.text())).toStrictEqual([
                taskLabel,
                taskDescription,
                "Created < 1 minute ago by unknown user. Last modified < 1 minute ago by unknown user.",
            ]);
        });
    });
});
