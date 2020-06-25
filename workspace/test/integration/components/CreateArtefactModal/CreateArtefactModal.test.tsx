import React from "react";
import "@testing-library/jest-dom";
import mockAxios from "../../../__mocks__/axios";
import { ReactWrapper } from "enzyme";
import { SERVE_PATH } from "../../../../src/app/constants/path";
import { createBrowserHistory } from "history";
import { logRequests, testWrapper } from "../../TestHelper";
import { CreateArtefactModal } from "../../../../src/app/views/shared/modals/CreateArtefactModal/CreateArtefactModal";

describe("Task creation widget", () => {
    afterEach(() => {
        mockAxios.reset();
    });

    const PROJECT_ID = "projectId";
    const TASK_ID = "taskId";

    const createArtefactWrapper = (currentUrl: string = `${SERVE_PATH}`) => {
        const history = createBrowserHistory();
        history.location.pathname = currentUrl;
        return testWrapper(<CreateArtefactModal />, history, {
            common: {
                artefactModal: {
                    isOpen: true,
                },
            },
        });
    };

    it("should show only the project artefact to select when on the main search page", async () => {
        const wrapper = createArtefactWrapper();
        logRequests();
    });
});
