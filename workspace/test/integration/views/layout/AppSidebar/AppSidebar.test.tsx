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

    const renderSidebar = (initialSettings: object = {}) =>
        renderWrapper(
            <shadcn.SidebarProvider>
                <AppSidebar />
            </shadcn.SidebarProvider>,
            history,
            {
                common: { initialSettings },
            },
        );

    afterEach(() => {
        wrapper.unmount();
        mockAxios.reset();
    });

    it("renders the DI navigation items", () => {
        history.push(workspacePath(""));
        wrapper = renderSidebar();
        ["Projects", "Datasets", "Workflows", "Activities"].forEach((label) => {
            expect(wrapper.getByText(label)).toBeInTheDocument();
        });
    });

    it("marks the current item type as active", () => {
        history.push(workspacePath("") + "?itemType=dataset");
        wrapper = renderSidebar();
        const datasetLink = wrapper.getByText("Datasets").closest("a") as HTMLElement;
        expect(datasetLink.getAttribute("data-active")).toBe("true");
        const projectLink = wrapper.getByText("Projects").closest("a") as HTMLElement;
        expect(projectLink.getAttribute("data-active")).toBe("false");
    });

    it("only shows the DM section when a DM base URL is configured", () => {
        history.push(workspacePath(""));
        wrapper = renderSidebar();
        expect(wrapper.queryByText("Knowledge graphs")).not.toBeInTheDocument();
        wrapper.unmount();
        wrapper = renderSidebar({ dmBaseUrl: "http://docker.local" });
        expect(wrapper.getByText("Knowledge graphs")).toBeInTheDocument();
    });
});
