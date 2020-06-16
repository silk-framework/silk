import React from "react";
import "@testing-library/jest-dom";
import { createBrowserHistory } from "history";
import mockAxios from "../../__mocks__/axios";
import { logRequests, testWrapper } from "../TestHelper";
import { Workspace } from "../../../src/app/views/pages/Workspace/Workspace";

describe("Project page", () => {
    let hostPath = process.env.HOST;
    afterEach(() => {
        mockAxios.reset();
    });

    const loadProjectPage = () => {
        const history = createBrowserHistory();
        history.location.pathname = "/dataintegration/workspace-beta/projects/cmem";

        testWrapper(<Workspace />, history);
    };

    it("should get common data types or for specific project", async () => {
        loadProjectPage();
        logRequests(mockAxios);
        const reqInfo = mockAxios.getReqMatching({
            url: hostPath + "/api/workspace/searchConfig/types?projectId=cmem",
        });
        expect(reqInfo).toBeTruthy();
    });

    it("should request meta data", async () => {
        loadProjectPage();
        const reqInfo = mockAxios.getReqMatching({
            url: hostPath + "/api/workspace/projects/cmem/metaData",
        });
        expect(reqInfo).toBeTruthy();
    });

    xit("should send the right projectId to backend", async () => {
        loadProjectPage();
        const reqInfo = mockAxios.getReqMatching({
            url: hostPath + "/api/workspace/searchItems",
        });
        expect(reqInfo.data).toEqual({
            limit: 10,
            offset: 0,
            project: "cmem",
            textQuery: "",
            facets: [],
        });
    });
});
