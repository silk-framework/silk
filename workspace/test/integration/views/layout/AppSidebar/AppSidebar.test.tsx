import React from "react";
import { createBrowserHistory } from "history";
import { act, RenderResult, waitFor } from "@testing-library/react";
import { shadcn } from "@eccenca/gui-elements";
import mockAxios from "../../../../__mocks__/axios";
import { mockedAxiosResponse, renderWrapper, workspacePath } from "../../../TestHelper";
import { AppSidebar } from "../../../../../src/app/views/layout/AppSidebar/AppSidebar";
import { readStoredSidebarOpen } from "../../../../../src/app/views/layout/AppLayout/AppLayout";

describe("AppSidebar", () => {
    let wrapper: RenderResult;
    const history = createBrowserHistory();

    const renderSidebar = (common: object = {}) =>
        renderWrapper(
            // TooltipProvider mirrors the app shell: the pristine radix-nova sidebar no
            // longer mounts its own provider for the collapsed-rail tooltips.
            <shadcn.TooltipProvider>
                <shadcn.SidebarProvider>
                    <AppSidebar />
                </shadcn.SidebarProvider>
            </shadcn.TooltipProvider>,
            history,
            {
                common: { initialSettings: {}, ...common },
            },
        );

    afterEach(() => {
        wrapper.unmount();
        mockAxios.reset();
    });

    it("shows the switcher tile plus Tasks/Activities (global fallback) with no project in context", () => {
        history.push(workspacePath(""));
        wrapper = renderSidebar();
        expect(wrapper.getByText("Select a project")).toBeInTheDocument();
        // Tasks/Activities stay visible and point at the global workbench / activities routes.
        const tasksLink = wrapper.getByText("Tasks").closest("a") as HTMLElement;
        expect(tasksLink.getAttribute("href")).toBe(workspacePath(""));
        const activitiesLink = wrapper.getByText("Activities").closest("a") as HTMLElement;
        expect(activitiesLink.getAttribute("href")).toBe(workspacePath("/activities"));
        const datasetsLink = wrapper.getByText("Datasets").closest("a") as HTMLElement;
        expect(datasetsLink.getAttribute("href")).toBe(workspacePath("") + "?itemType=dataset");
    });

    it("marks Datasets (not Tasks) active on the dataset-filtered workbench list", () => {
        history.push(workspacePath("") + "?itemType=dataset");
        wrapper = renderSidebar();
        const datasetsLink = wrapper.getByText("Datasets").closest("a") as HTMLElement;
        expect(datasetsLink.getAttribute("data-active")).toBe("true");
        // Without a project the Tasks item shares the workbench pathname — it must yield.
        const tasksLink = wrapper.getByText("Tasks").closest("a") as HTMLElement;
        expect(tasksLink.getAttribute("data-active")).toBe("false");
    });

    it("scopes Tasks and Activities to the project when one is open", () => {
        history.push(workspacePath("/projects/myproject"));
        wrapper = renderSidebar({ currentProjectId: "myproject" });
        // Falls back to the project id as label until the project list resolves.
        expect(wrapper.getByText("myproject")).toBeInTheDocument();
        const tasksLink = wrapper.getByText("Tasks").closest("a") as HTMLElement;
        expect(tasksLink.getAttribute("href")).toBe(workspacePath("/projects/myproject"));
        expect(tasksLink.getAttribute("data-active")).toBe("true");
        expect(wrapper.getByText("Activities")).toBeInTheDocument();
    });

    it("marks project Activities active on the activities route", () => {
        history.push(workspacePath("/projects/myproject/activities"));
        wrapper = renderSidebar({ currentProjectId: "myproject" });
        const activitiesLink = wrapper.getByText("Activities").closest("a") as HTMLElement;
        expect(activitiesLink.getAttribute("data-active")).toBe("true");
        const tasksLink = wrapper.getByText("Tasks").closest("a") as HTMLElement;
        expect(tasksLink.getAttribute("data-active")).toBe("false");
    });

    it("only shows the DM section when a DM base URL is configured", () => {
        history.push(workspacePath(""));
        wrapper = renderSidebar();
        expect(wrapper.queryByText("Knowledge graphs")).not.toBeInTheDocument();
        wrapper.unmount();
        wrapper = renderSidebar({ initialSettings: { dmBaseUrl: "http://docker.local" } });
        expect(wrapper.getByText("Knowledge graphs")).toBeInTheDocument();
    });

    // Single-project auto-select (NavProject): "if there is exactly one project it should be
    // automatically selected", but ONLY on the dashboard landing route with an empty query string.
    describe("single-project auto-select", () => {
        const hostPath = process.env.HOST;
        const searchItemsUrl = hostPath + "/api/workspace/searchItems";

        it("auto-selects the only project on the dashboard route with an empty query string (both guards satisfied)", async () => {
            history.push(workspacePath(""));
            wrapper = renderSidebar();
            // The count is all that matters, so a minimal limit is requested.
            const req = mockAxios.getReqMatching({ url: searchItemsUrl });
            expect(req).toBeTruthy();
            expect(req.data).toEqual({ itemType: "project", limit: 1 });
            mockAxios.mockResponseFor(
                { url: searchItemsUrl },
                mockedAxiosResponse({ data: { total: 1, results: [{ id: "only-project" }] } }),
            );
            await waitFor(() => {
                expect(history.location.pathname).toBe(workspacePath("/projects/only-project"));
            });
        });

        it("does NOT auto-select when more than one project exists", async () => {
            history.push(workspacePath(""));
            wrapper = renderSidebar();
            expect(mockAxios.getReqMatching({ url: searchItemsUrl })).toBeTruthy();
            // Fully drain the resolved request's promise chain, then assert the route stayed put.
            await act(async () => {
                mockAxios.mockResponseFor(
                    { url: searchItemsUrl },
                    mockedAxiosResponse({
                        data: { total: 2, results: [{ id: "p1" }, { id: "p2" }] },
                    }),
                );
                // Drain the mocked-response promise chain (a fixed number of hops, not a timed wait).
                await new Promise((resolve) => setTimeout(resolve, 0));
            });
            expect(history.location.pathname).toBe(workspacePath(""));
        });

        it("does NOT auto-select when the dashboard route carries a query string (empty-query guard)", () => {
            history.push(workspacePath("") + "?itemType=project");
            wrapper = renderSidebar();
            // The guard returns before any request is issued.
            expect(mockAxios.getReqMatching({ url: searchItemsUrl })).toBeFalsy();
            expect(history.location.pathname).toBe(workspacePath(""));
        });

        it("does NOT auto-select outside the dashboard landing route (route guard)", () => {
            history.push(workspacePath("/activities"));
            wrapper = renderSidebar();
            expect(mockAxios.getReqMatching({ url: searchItemsUrl })).toBeFalsy();
            expect(history.location.pathname).toBe(workspacePath("/activities"));
        });
    });
});

describe("AppLayout sidebar_state cookie restore", () => {
    const clearSidebarCookie = () => {
        document.cookie = "sidebar_state=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/";
    };

    beforeEach(clearSidebarCookie);
    afterEach(clearSidebarCookie);

    it("restores the open state from a sidebar_state=true cookie", () => {
        document.cookie = "sidebar_state=true; path=/";
        expect(readStoredSidebarOpen()).toBe(true);
    });

    it("restores the collapsed state from a sidebar_state=false cookie", () => {
        document.cookie = "sidebar_state=false; path=/";
        expect(readStoredSidebarOpen()).toBe(false);
    });

    it("falls back to closed when no sidebar_state cookie is present", () => {
        expect(readStoredSidebarOpen()).toBe(false);
    });

    it("reads the sidebar_state value even when surrounded by other cookies", () => {
        document.cookie = "foo=bar; path=/";
        document.cookie = "sidebar_state=true; path=/";
        document.cookie = "baz=qux; path=/";
        expect(readStoredSidebarOpen()).toBe(true);
    });
});
