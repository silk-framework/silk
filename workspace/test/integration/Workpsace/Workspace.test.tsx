import React from "react";
import "@testing-library/jest-dom";
import qs from "qs";
import { createMemoryHistory } from "history";
import { Provider } from "react-redux";
import { ConnectedRouter } from "connected-react-router";
import mockAxios from "../../__mocks__/axios";
import {
    byTestId,
    createStore,
    findAllDOMElements,
    mockedAxiosResponse,
    renderWrapper,
    workspacePath,
} from "../TestHelper";
import { Workspace } from "../../../src/app/views/pages/Workspace/Workspace";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { GlobalContextsWrapper } from "../../../src/app/GlobalContextsWrapper";
import { useWorkbenchListState } from "../../../src/app/hooks/useWorkbenchListState";
import { GlobalTableTypes } from "../../../src/app/hooks/useStoreGlobalTableSettings";
import ViewModeToggle from "../../../src/app/views/pages/Workspace/Toolbar/ViewModeToggle";
import WorkspaceFilters from "../../../src/app/views/pages/Workspace/Toolbar/WorkspaceFilters";
import SearchTable from "../../../src/app/views/shared/SearchList/SearchTable";
import SearchGrid from "../../../src/app/views/shared/SearchList/SearchGrid";
import { ISearchResultsServer } from "../../../src/app/store/ducks/workspace/typings";

describe("Search Items", () => {
    let hostPath = process.env.HOST;
    let history = null;
    const resultData = {
        description: "123123",
        id: "ID-",
        itemLinks: [
            {
                label: "Project details page",
                path: "/workbench/projects/eb233297-9d72-4b82-b77c-b2d1ee193c29_NewProject",
            },
        ],
        label: "New Project",
        type: "project",
    };

    const mockItemTypesRequest = () => {
        mockAxios.mockResponseFor(
            {
                url: hostPath + "/api/workspace/searchConfig/types",
            },
            mockedAxiosResponse({
                data: {
                    label: "Datatypes",
                    values: [{ id: "dataset", label: "dataset" }],
                },
            }),
        );
    };

    const mockSearchItemsRequest = () => {
        let arr = [];
        for (let i = 0; i < 20; i++) {
            arr.push({
                ...resultData,
                id: resultData.id + i,
            });
        }

        mockAxios.mockResponseFor(
            {
                url: hostPath + "/api/workspace/searchItems",
            },
            mockedAxiosResponse({
                data: {
                    facets: [
                        {
                            id: "facetId",
                            label: "FACET_LABEL",
                            type: "keyword",
                            values: [
                                { id: "test1", label: "test1" },
                                { id: "test2", label: "test2" },
                            ],
                        },
                    ],
                    results: arr,
                    sortByProperties: [
                        {
                            id: "label",
                            label: "Label",
                        },
                    ],
                    total: 20,
                },
            }),
        );
    };

    const getWrapper = (searchParams?: Object) => {
        history = createMemoryHistory();
        const rootPath = workspacePath("");
        history.push(rootPath);
        if (searchParams) {
            const qsStr = qs.stringify(searchParams, { arrayFormat: "comma" });
            history.push(`${rootPath}?${qsStr}`);
        }
        return renderWrapper(<Workspace />, history);
    };

    afterEach(() => {
        mockAxios.reset();
    });

    it("should filter items, by given criteria from URL search params", async () => {
        const filteredQueryParams = {
            textQuery: "some text",
            itemType: "dataset",
            page: 2,
            f_ids: ["facetId1", "facetId2"],
            f_keys: ["facet1Key1|facet1Key2", "facet2Key"],
            types: ["keyword", "keyword"],
        };

        getWrapper(filteredQueryParams);

        const reqInfo = mockAxios.getReqMatching({
            url: hostPath + "/api/workspace/searchItems",
        });
        expect(reqInfo.data).toEqual({
            textQuery: "some text",
            itemType: "dataset",
            limit: 10,
            offset: 10,
            sortOrder: "ASC",
            facets: [
                { facetId: "facetId1", type: "keyword", keywordIds: ["facet1Key1", "facet1Key2"] },
                { facetId: "facetId2", type: "keyword", keywordIds: ["facet2Key"] },
            ],
        });
    });

    it("should item types requested", () => {
        getWrapper();

        const reqInfo = mockAxios.getReqMatching({
            url: hostPath + "/api/workspace/searchConfig/types",
        });
        expect(reqInfo).toBeTruthy();
    });

    it("should facets presented correctly", async () => {
        const filteredQueryParams = {
            itemType: "dataset",
        };

        const wrapper = getWrapper(filteredQueryParams);

        mockItemTypesRequest();

        mockSearchItemsRequest();

        // Facets are surfaced as filter dropdown "option menus" in the toolbar (one per available facet).
        await waitFor(() => {
            const facetMenu = findAllDOMElements(wrapper, byTestId(`filter-menu-facetId`));
            expect(facetMenu).toHaveLength(1);
            expect(facetMenu[0].textContent).toContain("FACET_LABEL");
        });

        // Opening the menu shows one entry per facet value. The dropdown renders in a portal
        // outside the wrapper container, hence the `screen` queries.
        const user = userEvent.setup();
        await user.click(findAllDOMElements(wrapper, byTestId("filter-menu-facetId"))[0]);
        expect(await screen.findByRole("menuitem", { name: "test1" })).toBeVisible();
        expect(screen.getByRole("menuitem", { name: "test2" })).toBeVisible();

        // Toggling a value fires a new search request with the facet applied ...
        await user.click(screen.getByRole("menuitem", { name: "test1" }));
        await waitFor(() => {
            const reqInfo = mockAxios.getReqMatching({
                url: hostPath + "/api/workspace/searchItems",
            });
            expect(reqInfo).toBeTruthy();
            expect(reqInfo.data.facets).toEqual([{ facetId: "facetId", type: "keyword", keywordIds: ["test1"] }]);
        });

        // ... and keeps the (multi-select) facet menu open, so several values can be toggled in one go.
        expect(screen.getByRole("menuitem", { name: "test1" })).toBeVisible();
        expect(screen.getByRole("menuitem", { name: "test2" })).toBeVisible();
    });
});

// ---------------------------------------------------------------------------------------------
// Workbench view mode (table <-> grid) — the toggle, its persistence under the per-surface
// GlobalTableContext key, and the two result presentations (SearchTable / SearchGrid).
// ---------------------------------------------------------------------------------------------

describe("Workbench view mode", () => {
    it("ViewModeToggle marks the active mode and reports the other on click", () => {
        const onChange = jest.fn();
        const wrapper = renderWrapper(<ViewModeToggle mode="table" onChange={onChange} />);
        const tableBtn = wrapper.container.querySelector(byTestId("view-mode-table")) as HTMLElement;
        const gridBtn = wrapper.container.querySelector(byTestId("view-mode-grid")) as HTMLElement;
        expect(tableBtn.getAttribute("aria-pressed")).toBe("true");
        expect(gridBtn.getAttribute("aria-pressed")).toBe("false");

        fireEvent.click(gridBtn);
        expect(onChange).toHaveBeenCalledWith("grid");

        wrapper.unmount();
        // With grid active the pressed state flips over to the grid button.
        const gridActive = renderWrapper(<ViewModeToggle mode="grid" onChange={onChange} />);
        expect(gridActive.container.querySelector(byTestId("view-mode-grid"))!.getAttribute("aria-pressed")).toBe(
            "true",
        );
    });

    describe("persistence via GlobalTableContext", () => {
        // A minimal surface that consumes the shared list-state hook, so several of them can share a
        // single GlobalContextsWrapper and we can watch their view modes independently.
        const Surface = ({ tableKey }: { tableKey: GlobalTableTypes }) => {
            const { viewMode, handleViewModeChange } = useWorkbenchListState(tableKey);
            return (
                <div data-test-id={`surface-${tableKey}`}>
                    <span data-test-id={`vm-${tableKey}`}>{viewMode}</span>
                    <ViewModeToggle mode={viewMode} onChange={handleViewModeChange} />
                </div>
            );
        };

        beforeEach(() => localStorage.clear());
        afterEach(() => localStorage.clear());

        it("toggling the /workbench surface persists under the 'workbench' key without touching 'projectContents'", () => {
            const wrapper = renderWrapper(
                <GlobalContextsWrapper>
                    <Surface tableKey="workbench" />
                    <Surface tableKey="projectContents" />
                </GlobalContextsWrapper>,
            );

            // Both surfaces default to the table view.
            expect(wrapper.container.querySelector(byTestId("vm-workbench"))!.textContent).toBe("table");
            expect(wrapper.container.querySelector(byTestId("vm-projectContents"))!.textContent).toBe("table");

            const workbenchSurface = wrapper.container.querySelector(byTestId("surface-workbench")) as HTMLElement;
            fireEvent.click(workbenchSurface.querySelector(byTestId("view-mode-grid")) as HTMLElement);

            // Only the workbench surface flips to grid; the projectContents surface is unaffected.
            expect(wrapper.container.querySelector(byTestId("vm-workbench"))!.textContent).toBe("grid");
            expect(wrapper.container.querySelector(byTestId("vm-projectContents"))!.textContent).toBe("table");

            // ... and the persisted settings keep the two surfaces' view modes independent.
            const stored = JSON.parse(localStorage.getItem("global_table_settings") as string);
            expect(stored.workbench.viewMode).toBe("grid");
            expect(stored.projectContents.viewMode).toBeUndefined();
        });

        it("toggling the Project 'Contents' surface persists under the 'projectContents' key only", () => {
            const wrapper = renderWrapper(
                <GlobalContextsWrapper>
                    <Surface tableKey="workbench" />
                    <Surface tableKey="projectContents" />
                </GlobalContextsWrapper>,
            );

            const contentsSurface = wrapper.container.querySelector(byTestId("surface-projectContents")) as HTMLElement;
            fireEvent.click(contentsSurface.querySelector(byTestId("view-mode-grid")) as HTMLElement);

            expect(wrapper.container.querySelector(byTestId("vm-projectContents"))!.textContent).toBe("grid");
            expect(wrapper.container.querySelector(byTestId("vm-workbench"))!.textContent).toBe("table");

            const stored = JSON.parse(localStorage.getItem("global_table_settings") as string);
            expect(stored.projectContents.viewMode).toBe("grid");
            expect(stored.workbench.viewMode).toBeUndefined();
        });
    });

    describe("SearchTable / SearchGrid presentation", () => {
        const items: ISearchResultsServer[] = [
            {
                id: "t1",
                label: "Alpha",
                type: "transform",
                projectId: "p1",
                description: "",
                itemLinks: [{ label: "Editor", path: "/transform/p1/t1/editor" }],
            } as ISearchResultsServer,
            {
                id: "t2",
                label: "Beta",
                type: "workflow",
                projectId: "p1",
                description: "",
                itemLinks: [],
            } as ISearchResultsServer,
        ];
        const callbacks = {
            onOpenDeleteModal: jest.fn(),
            onOpenDuplicateModal: jest.fn(),
            onOpenCopyToModal: jest.fn(),
            toggleShowIdentifierModal: jest.fn(),
        };

        it("SearchTable renders one row per item and shows the Project column at global scope", () => {
            const wrapper = renderWrapper(<SearchTable data={items} {...callbacks} />);
            expect(wrapper.container.querySelectorAll(byTestId("search-item"))).toHaveLength(2);
            // Type, Name, Description, Plugin, Project, Tags, Actions
            expect(wrapper.container.querySelectorAll("thead th")).toHaveLength(7);
            expect(wrapper.getByText("Project")).toBeInTheDocument();
        });

        it("SearchTable hides the Project column when a parentProjectId is given", () => {
            const wrapper = renderWrapper(<SearchTable data={items} parentProjectId="p1" {...callbacks} />);
            expect(wrapper.container.querySelectorAll("thead th")).toHaveLength(6);
            expect(wrapper.queryByText("Project")).not.toBeInTheDocument();
        });

        it("SearchTable drops the bordered panel when rendered flush", () => {
            const framed = renderWrapper(<SearchTable data={items} {...callbacks} />);
            expect(framed.container.querySelector(byTestId("search-result-table"))!.className).toContain("border");
            framed.unmount();

            const flush = renderWrapper(<SearchTable data={items} flush {...callbacks} />);
            expect(flush.container.querySelector(byTestId("search-result-table"))!.className).not.toContain("border");
        });

        it("SearchGrid renders one card per item", () => {
            const wrapper = renderWrapper(<SearchGrid data={items} {...callbacks} />);
            expect(wrapper.container.querySelector(byTestId("search-result-grid"))).toBeInTheDocument();
            expect(wrapper.container.querySelectorAll(".diapp-searchcard")).toHaveLength(2);
        });
    });
});

// ---------------------------------------------------------------------------------------------
// WorkspaceFilters — the item-type filter dropdown: re-selecting the active type clears it, and
// the Project/Global options are hidden inside a project.
// ---------------------------------------------------------------------------------------------

describe("WorkspaceFilters item type filter", () => {
    const typeModifier = (options: { id: string; label: string }[]) => ({
        common: { availableDataTypes: { type: { label: "Item type", field: "itemType", options } } },
    });

    const renderFilters = (props: React.ComponentProps<typeof WorkspaceFilters>, state: Record<string, unknown>) => {
        const history = createMemoryHistory();
        history.push(workspacePath(""));
        const store = createStore(history, state as never);
        const utils = render(
            <Provider store={store}>
                <ConnectedRouter history={history}>
                    <WorkspaceFilters {...props} />
                </ConnectedRouter>
            </Provider>,
        );
        return { store, container: utils.container };
    };

    it("clears the item type filter when the already-active type is selected again", async () => {
        const { store, container } = renderFilters(
            {},
            {
                ...typeModifier([
                    { id: "alpha", label: "Alpha" },
                    { id: "beta", label: "Beta" },
                ]),
                workspace: { filters: { appliedFilters: { textQuery: "", itemType: "alpha" } } },
            },
        );
        const user = userEvent.setup();
        await user.click(container.querySelector(byTestId("filter-menu-itemType")) as HTMLElement);
        await user.click(await screen.findByRole("menuitem", { name: "Alpha" }));

        expect(store.getState().workspace.filters.appliedFilters.itemType).toBeUndefined();
    });

    it("applies a different item type when a non-active option is selected", async () => {
        const { store, container } = renderFilters(
            {},
            {
                ...typeModifier([
                    { id: "alpha", label: "Alpha" },
                    { id: "beta", label: "Beta" },
                ]),
                workspace: { filters: { appliedFilters: { textQuery: "", itemType: "alpha" } } },
            },
        );
        const user = userEvent.setup();
        await user.click(container.querySelector(byTestId("filter-menu-itemType")) as HTMLElement);
        await user.click(await screen.findByRole("menuitem", { name: "Beta" }));

        expect(store.getState().workspace.filters.appliedFilters.itemType).toBe("beta");
    });

    it("hides the Project and Global options when rendered inside a project", async () => {
        const state = typeModifier([
            { id: "alpha", label: "Alpha" },
            { id: "project", label: "Project" },
        ]);
        const extra = [{ id: "global", label: "Global" }];
        const user = userEvent.setup();

        // Inside a project: Project + Global are filtered out, ordinary types remain.
        const scoped = renderFilters({ projectId: "p1", extraItemTypeModifiers: extra }, state);
        await user.click(scoped.container.querySelector(byTestId("filter-menu-itemType")) as HTMLElement);
        expect(await screen.findByRole("menuitem", { name: "Alpha" })).toBeVisible();
        expect(screen.queryByRole("menuitem", { name: "Project" })).not.toBeInTheDocument();
        expect(screen.queryByRole("menuitem", { name: "Global" })).not.toBeInTheDocument();
    });

    it("keeps the Project and Global options at global scope (no project)", async () => {
        const state = typeModifier([
            { id: "alpha", label: "Alpha" },
            { id: "project", label: "Project" },
        ]);
        const extra = [{ id: "global", label: "Global" }];
        const user = userEvent.setup();

        const global = renderFilters({ extraItemTypeModifiers: extra }, state);
        await user.click(global.container.querySelector(byTestId("filter-menu-itemType")) as HTMLElement);
        expect(await screen.findByRole("menuitem", { name: "Project" })).toBeVisible();
        expect(screen.getByRole("menuitem", { name: "Global" })).toBeVisible();
    });
});
