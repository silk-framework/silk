import React from "react";
import { createBrowserHistory } from "history";
import { RenderResult } from "@testing-library/react";
import { shadcn } from "@eccenca/gui-elements";
import mockAxios from "../../../../__mocks__/axios";
import { renderWrapper, workspacePath } from "../../../TestHelper";
import { AppSidebar } from "../../../../../src/app/views/layout/AppSidebar/AppSidebar";

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
});
