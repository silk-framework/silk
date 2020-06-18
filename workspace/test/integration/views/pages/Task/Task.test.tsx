import React from "react";
import mockAxios from "../../../../__mocks__/axios";
import {
    apiUrl,
    legacyApiUrl,
    checkRequestMade,
    testWrapper,
    workspacePath,
    setUseParams,
    mockedAxiosResponse,
    eventually,
    findSingleElement,
    findSingleElementByTestId,
    findAll,
} from "../../../TestHelper";
import { createBrowserHistory } from "history";
import Task from "../../../../../src/app/views/pages/Task";
import {
    requestArtefactPropertiesTestResponse,
    requestTaskDataTestResponse,
    ParameterDescriptionGenerator,
    ITaskParameter,
} from "../../../requests/sharedResponseStubs";
import { waitFor } from "@testing-library/react";
import { ReactWrapper } from "enzyme";
import { IArtefactItemProperty } from "../../../../../src/app/store/ducks/common/typings";

describe("Task page", () => {
    afterEach(() => {
        mockAxios.reset();
    });

    const PROJECT_ID = "cmem";
    const TASK_ID = "taskId";
    const pluginId = "testPlugin";
    const pluginLabel = "Test Plugin";
    const taskDataUrl = legacyApiUrl(`/workspace/projects/${PROJECT_ID}/tasks/${TASK_ID}`);
    const pluginUrl = apiUrl(`/core/plugins/${pluginId}`);

    beforeAll(() => {
        setUseParams(PROJECT_ID, TASK_ID);
    });

    let taskPageWrapper: ReactWrapper<any, any> = null;
    beforeEach(() => {
        const history = createBrowserHistory();
        history.location.pathname = workspacePath(`/projects/${PROJECT_ID}/task/${TASK_ID}`);

        taskPageWrapper = testWrapper(<Task />, history);
    });

    it("should request meta data, related items and task config", async () => {
        checkRequestMade(legacyApiUrl(`/workspace/projects/${PROJECT_ID}/tasks/${TASK_ID}/metadata`));
        checkRequestMade(apiUrl(`/workspace/projects/${PROJECT_ID}/tasks/${TASK_ID}/relatedItems`));
        checkRequestMade(taskDataUrl, "GET", { withLabels: true });
        mockAxios.mockResponseFor(
            taskDataUrl,
            mockedAxiosResponse({ data: requestTaskDataTestResponse({ pluginId: pluginId }) })
        );
        await eventually(() => checkRequestMade(pluginUrl));
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
        await eventually(() => checkRequestMade(pluginUrl));
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
            const taskConfig = findSingleElementByTestId(taskPageWrapper, "taskConfigWidget");
            expect(findSingleElement(taskConfig, "header h3").text()).toContain(pluginLabel);
        });
        const taskConfig = findSingleElementByTestId(taskPageWrapper, "taskConfigWidget");
        const propertyLabels = findAll(taskConfig, ".ecc-card__content .ecc-label").map((elem) => elem.text());
        expect(propertyLabels).toStrictEqual(params.map(([paramId, paramLabel]) => paramLabel));
        const propertyValues = findAll(taskConfig, ".ecc-card__content .ecc-propertyvalue__value").map((elem) =>
            elem.text()
        );
        expect(propertyValues).toStrictEqual(
            params.map(([pluginId, pluginLabel, value, label]) => (label ? label : value))
        );
    });
});
