import mockAxios from "../../__mocks__/axios";
import { logRequests, testWrapper } from "../TestHelper";
import { Workspace } from "../../../src/app/views/pages/Workspace/Workspace";
import Project from "../../../src/app/views/pages/Project";

jest.mock("../../../src/app/store/configureStore", () => {
    // Works and lets you check for constructor calls:
    return {
        getHistory: jest.fn().mockImplementation(() => {
            return {
                location: {
                    pathname: "/workspace-beta/projects/cmem",
                },
            };
        }),
    };
});

describe("Project page", () => {
    let hostPath = process.env.HOST;
    afterEach(() => {
        mockAxios.reset();
    });

    it("should get common data types or for specific project", async () => {
        testWrapper(Workspace, {});
        logRequests(mockAxios);
        const reqInfo = mockAxios.getReqMatching({
            url: hostPath + "/api/workspace/searchConfig/types?projectId=cmem",
        });
        expect(reqInfo).toBeTruthy();
    });

    it("should request meta data", async () => {
        testWrapper(Project, {});
        const reqInfo = mockAxios.getReqMatching({
            url: hostPath + "/api/workspace/projects/cmem/metaData",
        });
        expect(reqInfo).toBeTruthy();
    });

    xit("should send the right projectId to backend", async () => {
        testWrapper(Workspace, {});
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
