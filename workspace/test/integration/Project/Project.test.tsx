import React from "react";
import { waitFor } from "@testing-library/react";
import mockAxios from "../../__mocks__/axios";
import {
    apiUrl,
    checkRequestMade,
    legacyApiUrl,
    testWrapper,
    withMount,
    workspacePath,
    findAll,
    findSingleElement,
    byTestId,
    mockAxiosResponse,
    changeValue,
    keyDown,
    logRequests,
} from "../TestHelper";
import { createBrowserHistory } from "history";
import Project from "../../../src/app/views/pages/Project";
import qs from "qs";

//jest.setTimeout(50000);

describe("Project page", () => {
    const testProjectId = "testproject";
    const reducerState = {
        common: {
            currentProjectId: testProjectId,
        },
        workspace: {
            widgets: {
                isEmptyPage: false,
                filesList: [
                    {
                        id: "file.csv",
                        formattedSize: "666",
                        formattedDate: "2020-10-08",
                        name: "file.csv",
                        size: 666,
                        modified: "2020-10-08",
                    },
                ],
                files: {
                    isLoading: false,
                    results: [
                        {
                            name: "file.csv",
                            size: 666,
                            modified: "2020-10-08",
                        },
                    ],
                    error: {},
                },
            },
        },
    };
    let hostPath = process.env.HOST;
    let projectPageWrapper: ReactWrapper<any, any> = null;
    beforeEach(() => {
        const history = createBrowserHistory();
        history.location.pathname = workspacePath("/projects/" + testProjectId);

        projectPageWrapper = withMount(testWrapper(<Project />, history, reducerState));

        return projectPageWrapper;
    });

    afterEach(() => {
        mockAxios.reset();
    });

    it("should get common data types or for specific project", async () => {
        checkRequestMade(apiUrl("/workspace/searchConfig/types?projectId=" + testProjectId));
    });

    it("should request meta data", async () => {
        checkRequestMade(apiUrl("/workspace/projects/" + testProjectId + "/metaData"));
    });

    xit("should get available resources for file widget", () => {
        checkRequestMade(legacyApiUrl("/workspace/projects/" + testProjectId + "/resources"));
    });

    it("should get prefixes for configuration widget", () => {
        checkRequestMade(apiUrl("/workspace/projects/" + testProjectId + "/prefixes"));
    });

    it("should filter items, by given criteria from URL search params", async () => {
        const filteredQueryParams = qs.stringify(
            {
                textQuery: "some text",
                itemType: "dataset",
                limit: 15,
                page: 2,
                f_ids: ["facetId1", "facetId2"],
                f_keys: ["facet1Key1|facet1Key2", "facet2Key"],
                types: ["keyword", "keyword"],
            },
            { arrayFormat: "comma" }
        );

        let history = createBrowserHistory();
        history.location.pathname = workspacePath("/projects/" + testProjectId);
        history.location.search = filteredQueryParams;

        withMount(testWrapper(<Project />, history));

        const expectedSearchResponse = {
            textQuery: "some text",
            itemType: "dataset",
            limit: 15,
            offset: 10,
            project: testProjectId,
            facets: [
                { facetId: "facetId1", type: "keyword", keywordIds: ["facet1Key1", "facet1Key2"] },
                { facetId: "facetId2", type: "keyword", keywordIds: ["facet2Key"] },
            ],
        };

        checkRequestMade(apiUrl("/workspace/searchItems"), "POST", expectedSearchResponse);
    });

    it("file widget is displayed", () => {
        const filewidget = findAll(projectPageWrapper, byTestId(`project-files-widget`));
        expect(filewidget).toHaveLength(1);
    });

    const setFilesForWidget = (files) => {
        mockAxiosResponse(legacyApiUrl("/workspace/projects/" + testProjectId + "/resources"), { data: files });
    };

    it("file search bar is shown when there are files", async () => {
        setFilesForWidget(reducerState.workspace.widgets.files.results);
        await waitFor(() => {
            const filesearchinput = findAll(projectPageWrapper, byTestId(`file-search-bar`));
            expect(filesearchinput).toHaveLength(1);
        });
    });

    it("file search bar is not shown but upload widget when there are no files", async () => {
        setFilesForWidget([]);
        await waitFor(() => {
            const filesearchinput = findAll(projectPageWrapper, byTestId(`file-search-bar`));
            const emptyfilesnotifcation = findAll(projectPageWrapper, byTestId(`project-files-widget-empty`));
            expect(filesearchinput).toHaveLength(0);
            expect(emptyfilesnotifcation).toHaveLength(1);
        });
    });

    it("file search bar never disappears when no results are shown", async () => {
        setFilesForWidget(reducerState.workspace.widgets.files.results);
        await waitFor(() => {
            const filesearchinputchange = findSingleElement(projectPageWrapper, byTestId(`file-search-bar`));
            changeValue(filesearchinputchange, "unknown-string");
            keyDown(filesearchinputchange, "Enter");
            //setFilesForWidget([]);
        });
        //logRequests();
        setFilesForWidget([]);
        await waitFor(() => {
            const filesearchinputtest = findAll(projectPageWrapper, byTestId(`file-search-bar`));
            expect(filesearchinputtest).toHaveLength(1);
        });
    });
});
