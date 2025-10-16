import React from "react";
import { RenderResult, waitFor } from "@testing-library/react";
import mockAxios from "../../__mocks__/axios";
import {
    apiUrl,
    byTestId,
    changeInputValue,
    checkRequestMade,
    findElement,
    pressKeyDown,
    legacyApiUrl,
    mockAxiosResponse,
    workspacePath,
    renderWrapper,
    findAllDOMElements,
} from "../TestHelper";
import { createBrowserHistory, History, LocationState } from "history";
import Project from "../../../src/app/views/pages/Project";
import qs from "qs";

//jest.setTimeout(50000);

describe("Project page", () => {
    const testProjectId = "testproject";
    const expectedFile = "file.csv";
    const reducerState = {
        common: {
            currentProjectId: testProjectId,
        },
        workspace: {
            widgets: {
                isEmptyPage: false,
                filesList: [
                    {
                        id: expectedFile,
                        formattedSize: "666",
                        formattedDate: "2020-10-08",
                        name: expectedFile,
                        size: 666,
                        modified: "2020-10-08",
                    },
                ],
                files: {
                    isLoading: false,
                    results: [
                        {
                            name: expectedFile,
                            size: 666,
                            modified: "2020-10-08",
                        },
                    ],
                    error: {},
                },
            },
        },
    };
    let projectPageWrapper: RenderResult = null;
    let history: History<LocationState> = null;
    beforeEach(() => {
        history = createBrowserHistory();
        history.location.pathname = workspacePath("/projects/" + testProjectId);

        projectPageWrapper = renderWrapper(<Project />, history, reducerState);
        return projectPageWrapper;
    });

    afterEach(() => {
        mockAxios.reset();
    });

    it("should get common data types or for specific project", async () => {
        checkRequestMade(apiUrl("/workspace/searchConfig/types?projectId=" + testProjectId));
    });

    it("should request meta data", async () => {
        checkRequestMade(apiUrl("/workspace/projects/" + testProjectId + "/metaDataExpanded"));
    });

    it("should get prefixes for configuration widget", () => {
        checkRequestMade(apiUrl("/workspace/projects/" + testProjectId + "/prefixes"));
    });

    it("should search items for that project", () => {
        checkRequestMade(apiUrl("/workspace/searchItems"), "POST", { project: testProjectId }, true);
    });

    it("should search items when switching from one project to another", async () => {
        const otherProject = "otherProject";
        checkRequestMade(apiUrl("/workspace/searchItems"), "POST", { project: testProjectId }, true);
        history.push(workspacePath("/projects/" + otherProject));
        await waitFor(() => {
            checkRequestMade(apiUrl("/workspace/searchItems"), "POST", { project: otherProject }, true);
        });
    });

    it("should filter items, by given criteria from URL search params", async () => {
        const filteredQueryParams = qs.stringify(
            {
                textQuery: "some text",
                itemType: "dataset",
                page: 2,
                f_ids: ["facetId1", "facetId2"],
                f_keys: ["facet1Key1|facet1Key2", "facet2Key"],
                types: ["keyword", "keyword"],
            },
            { arrayFormat: "comma" },
        );

        let history = createBrowserHistory();
        history.location.pathname = workspacePath("/projects/" + testProjectId);
        history.location.search = filteredQueryParams;

        renderWrapper(<Project />, history);

        const expectedSearchResponse = {
            textQuery: "some text",
            itemType: "dataset",
            limit: 10,
            offset: 10,
            project: testProjectId,
            sortOrder: "ASC",
            facets: [
                { facetId: "facetId1", type: "keyword", keywordIds: ["facet1Key1", "facet1Key2"] },
                { facetId: "facetId2", type: "keyword", keywordIds: ["facet2Key"] },
            ],
        };

        checkRequestMade(apiUrl("/workspace/searchItems"), "POST", expectedSearchResponse);
    });

    it("file widget is displayed", () => {
        expect(findAllDOMElements(projectPageWrapper, byTestId(`project-files-widget`))).toHaveLength(1);
    });

    const setFilesForWidget = (files) => {
        mockAxiosResponse(legacyApiUrl("/workspace/projects/" + testProjectId + "/resources"), { data: files });
    };

    it("file search bar is shown when there are files", async () => {
        setFilesForWidget(reducerState.workspace.widgets.files.results);
        await waitFor(() => {
            expect(findAllDOMElements(projectPageWrapper, byTestId(`file-search-bar`))).toHaveLength(1);
        });
    });

    it("file search bar is not shown but upload widget when there are no files", async () => {
        setFilesForWidget([]);
        await waitFor(() => {
            expect(findAllDOMElements(projectPageWrapper, byTestId(`file-search-bar`))).toHaveLength(0);
            expect(findAllDOMElements(projectPageWrapper, byTestId(`project-files-widget-empty`))).toHaveLength(1);
        });
    });

    it("file search bar never disappears when no results are shown", async () => {
        setFilesForWidget(reducerState.workspace.widgets.files.results);
        await waitFor(() => {
            const fileSearchInput = findElement(projectPageWrapper, byTestId(`file-search-bar`)) as HTMLInputElement;
            changeInputValue(fileSearchInput, "unknown-string");
            pressKeyDown(fileSearchInput); //Enter key is default
            //setFilesForWidget([]);
        });
        setFilesForWidget([]);
        await waitFor(() => {
            expect(findAllDOMElements(projectPageWrapper, byTestId(`file-search-bar`))).toHaveLength(1);
        });
    });

    it("should have a download link for a file resource", async () => {
        setFilesForWidget(reducerState.workspace.widgets.files.results);
        await waitFor(() => {
            expect(projectPageWrapper.container.innerHTML).toContain(expectedFile);
        });
        const downloadIcon = findElement(projectPageWrapper, byTestId("resource-download-btn"));
        expect(downloadIcon.tagName).toBe("A");
        expect(downloadIcon.getAttribute("href")).toContain(expectedFile);
    });
});
