import React from "react";
import mockAxios from "../../../../__mocks__/axios";
import {
    apiUrl,
    legacyApiUrl,
    logRequests,
    checkRequestMade,
    testWrapper,
    workspacePath,
    setUseParams,
    mockAxiosResponse,
    eventually,
} from "../../../TestHelper";
import { createBrowserHistory } from "history";
import Task from "../../../../../src/app/views/pages/Task";
import { requestTaskDataTestResponse } from "../../../requests/sharedResponseStubs";

describe("Task page", () => {
    afterEach(() => {
        mockAxios.reset();
    });

    const PROJECT_ID = "cmem";
    const TASK_ID = "taskId";

    beforeAll(() => {
        setUseParams(PROJECT_ID, TASK_ID);
    });

    const taskPage = () => {
        const history = createBrowserHistory();
        history.location.pathname = workspacePath(`/projects/${PROJECT_ID}/task/${TASK_ID}`);

        return testWrapper(<Task />, history);
    };

    it("should request meta data, related items and task config", async () => {
        taskPage();
        checkRequestMade(legacyApiUrl(`/workspace/projects/${PROJECT_ID}/tasks/${TASK_ID}/metadata`));
        checkRequestMade(apiUrl(`/workspace/projects/${PROJECT_ID}/tasks/${TASK_ID}/relatedItems`));
        const taskDataUrl = legacyApiUrl(`/workspace/projects/${PROJECT_ID}/tasks/${TASK_ID}`);
        checkRequestMade(taskDataUrl, "GET", { withLabels: true });
        const pluginId = "testPlugin";
        mockAxios.mockResponseFor(
            taskDataUrl,
            mockAxiosResponse({ data: requestTaskDataTestResponse({ pluginId: pluginId }) })
        );
        await eventually(() => checkRequestMade(apiUrl(`/core/plugins/${pluginId}`)));
    });
});
