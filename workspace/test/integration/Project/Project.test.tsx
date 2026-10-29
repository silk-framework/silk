import React from "react";
import { act, RenderResult, waitFor } from "@testing-library/react";
import mockAxios from "../../__mocks__/axios";
import {
    apiUrl,
    byTestId,
    changeInputValue,
    checkRequestMade,
    clickRenderedElement,
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
import { GlobalTableContext } from "../../../src/app/GlobalContextsWrapper";
import {
    defaultGlobalTableSettings,
    GlobalTableBaseConfig,
    GlobalTableTypes,
} from "../../../src/app/hooks/useStoreGlobalTableSettings";

//jest.setTimeout(50000);

jest.mock("../../../src/app/views/shared/SearchList", () => ({
    __esModule: true,
    default: () => null,
}));

jest.mock("../../../src/app/views/shared/VariablesWidget/VariablesWidget", () => ({
    __esModule: true,
    default: () => null,
}));

jest.mock("../../../src/app/views/pages/Project/ActivityInfoWidget", () => ({
    __esModule: true,
    default: () => null,
}));

jest.mock("../../../src/app/views/pages/Project/DeprecatedPlugins/DeprecatedPluginsWidget", () => ({
    __esModule: true,
    DeprecatedPluginsWidget: () => null,
}));

jest.mock("../../../src/app/views/pages/Project/WarningWidget/WarningWidget", () => ({
    ProjectTaskLoadingErrors: () => null,
}));

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
    let projectPageWrapper: RenderResult | null = null;
    let history: History<LocationState> | null = null;

    const TestGlobalTableProvider = ({ children }: { children: React.ReactNode }) => {
        const [globalTableSettings, setGlobalTableSettings] = React.useState(defaultGlobalTableSettings);

        const updateGlobalTableSettings = React.useCallback(
            (settings: GlobalTableBaseConfig, explicitKey?: GlobalTableTypes) => {
                const tableKey = explicitKey ?? "workbench";
                setGlobalTableSettings((currentSettings) => ({
                    ...currentSettings,
                    [tableKey]: {
                        ...currentSettings[tableKey],
                        ...settings,
                    },
                }));
            },
            [],
        );

        return (
            <GlobalTableContext.Provider value={{ globalTableSettings, updateGlobalTableSettings }}>
                {children}
            </GlobalTableContext.Provider>
        );
    };

    const renderProjectPage = (
        customHistory: History<LocationState> = createBrowserHistory(),
        initialState = reducerState,
    ) => {
        customHistory.location.pathname = workspacePath("/projects/" + testProjectId);
        history = customHistory;
        projectPageWrapper = renderWrapper(
            <TestGlobalTableProvider>
                <Project />
            </TestGlobalTableProvider>,
            customHistory,
            initialState,
        );
        return projectPageWrapper;
    };

    afterEach(() => {
        projectPageWrapper?.unmount();
        projectPageWrapper = null;
        history = null;
        mockAxios.reset();
    });

    it("should get common data types or for specific project", async () => {
        renderProjectPage();
        checkRequestMade(apiUrl("/workspace/searchConfig/types?projectId=" + testProjectId));
    });

    it("should request meta data", async () => {
        renderProjectPage();
        checkRequestMade(apiUrl("/workspace/projects/" + testProjectId + "/metaDataExpanded"));
    });

    it("should get prefixes for configuration widget", () => {
        renderProjectPage();
        checkRequestMade(apiUrl("/workspace/projects/" + testProjectId + "/prefixes/detailed"));
    });

    it("should search items for that project", () => {
        renderProjectPage();
        checkRequestMade(apiUrl("/workspace/searchItems"), "POST", { project: testProjectId }, true);
    });

    it("should search items when switching from one project to another", async () => {
        const otherProject = "otherProject";
        renderProjectPage();
        checkRequestMade(apiUrl("/workspace/searchItems"), "POST", { project: testProjectId }, true);
        act(() => history.push(workspacePath("/projects/" + otherProject)));
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

        renderProjectPage(history);

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
        renderProjectPage();
        expect(findAllDOMElements(projectPageWrapper, byTestId(`project-files-widget`))).toHaveLength(1);
    });

    const setFilesForWidget = (files) => {
        mockAxiosResponse(legacyApiUrl("/workspace/projects/" + testProjectId + "/resources"), { data: files });
    };

    it("file search bar is shown when there are files", async () => {
        renderProjectPage();
        setFilesForWidget(reducerState.workspace.widgets.files.results);
        await waitFor(() => {
            expect(findAllDOMElements(projectPageWrapper, byTestId(`file-search-bar`))).toHaveLength(1);
        });
    });

    it("file search bar is not shown but upload widget when there are no files", async () => {
        renderProjectPage();
        setFilesForWidget([]);
        await waitFor(() => {
            expect(findAllDOMElements(projectPageWrapper, byTestId(`file-search-bar`))).toHaveLength(0);
            expect(findAllDOMElements(projectPageWrapper, byTestId(`project-files-widget-empty`))).toHaveLength(1);
        });
    });

    it("file search bar never disappears when no results are shown", async () => {
        renderProjectPage();
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
        renderProjectPage();
        setFilesForWidget(reducerState.workspace.widgets.files.results);
        await waitFor(() => {
            expect(projectPageWrapper.container.innerHTML).toContain(expectedFile);
        });
        const downloadIcon = findElement(projectPageWrapper, byTestId("resource-download-btn"));
        expect(downloadIcon.tagName).toBe("A");
        expect(downloadIcon.getAttribute("href")).toContain(expectedFile);
    });

    it("should sort files by size ascending, descending and then reset", async () => {
        renderProjectPage();
        const files = [
            { name: "alpha.csv", size: 30, modified: "2020-10-08T00:00:00Z" },
            { name: "beta.csv", size: 10, modified: "2020-10-09T00:00:00Z" },
            { name: "gamma.csv", size: 20, modified: "2020-10-10T00:00:00Z" },
        ];
        setFilesForWidget(files);

        const fileNamesInTable = () =>
            Array.from(projectPageWrapper.container.querySelectorAll('[data-test-id="project-files-widget"] tbody tr'))
                .map((row) => row.querySelector("td")?.textContent?.trim())
                .filter((value): value is string => !!value);

        await waitFor(() => {
            expect(fileNamesInTable()).toEqual(["alpha.csv", "beta.csv", "gamma.csv"]);
        });

        clickRenderedElement(findElement(projectPageWrapper, byTestId("project-files-sort-size")));
        await waitFor(() => {
            expect(fileNamesInTable()).toEqual(["beta.csv", "gamma.csv", "alpha.csv"]);
        });

        clickRenderedElement(findElement(projectPageWrapper, byTestId("project-files-sort-size")));
        await waitFor(() => {
            expect(fileNamesInTable()).toEqual(["alpha.csv", "gamma.csv", "beta.csv"]);
        });

        clickRenderedElement(findElement(projectPageWrapper, byTestId("project-files-sort-size")));
        await waitFor(() => {
            expect(fileNamesInTable()).toEqual(["alpha.csv", "beta.csv", "gamma.csv"]);
        });
    });
});
