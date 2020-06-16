import React from "react";
import mockAxios from "../../__mocks__/axios";
import { testWrapper } from "../TestHelper";
import { createBrowserHistory } from "history";
import Project from "../../../src/app/views/pages/Project";

describe("Project page", () => {
    let hostPath = process.env.HOST;
    afterEach(() => {
        mockAxios.reset();
    });

    const projectPage = () => {
        const history = createBrowserHistory();
        history.location.pathname = "/workspace-beta/projects/cmem";

        return testWrapper(<Project />, history);
    };

    it("should get common data types or for specific project", async () => {
        projectPage();
        const reqInfo = mockAxios.getReqMatching({
            url: hostPath + "/api/workspace/searchConfig/types?projectId=cmem",
        });
        expect(reqInfo).toBeTruthy();
    });

    it("should request meta data", async () => {
        projectPage();
        const reqInfo = mockAxios.getReqMatching({
            url: hostPath + "/api/workspace/projects/cmem/metaData",
        });
        expect(reqInfo).toBeTruthy();
    });

    it("should send the right projectId to backend", async () => {
        projectPage();
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
