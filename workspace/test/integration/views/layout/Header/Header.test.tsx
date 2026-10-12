import React from "react";
import { createBrowserHistory } from "history";
import mockAxios from "../../../../__mocks__/axios";
import { byTestId, mockedAxiosResponse, renderWrapper, setUseParams, workspacePath } from "../../../TestHelper";
import { Header } from "../../../../../src/app/views/layout/Header/Header";
import Task from "../../../../../src/app/views/pages/Task";
import { APP_VIEWHEADER_ID, PageHeader } from "../../../../../src/app/views/shared/PageHeader/PageHeader";
import { fireEvent, RenderResult, waitFor } from "@testing-library/react";
import { Helmet } from "react-helmet";
import { pluginRegistry, SUPPORTED_PLUGINS } from "../../../../../src/app/views/plugins/PluginRegistry";
import { BrandingProps } from "../../../../../src/app/views/plugins/plugin.types";

jest.mock("../../../../../src/app/views/shared/modals/CreateArtefactModal/CreateArtefactModal", () => ({
    CreateArtefactModal: () => null,
}));

jest.mock("../../../../../src/app/views/shared/ApplicationNotifications/NotificationsMenu", () => ({
    NotificationsMenu: () => null,
}));

jest.mock("../../../../../src/app/views/shared/TaskActivityOverview/TaskActivityOverview", () => ({
    TaskActivityOverview: () => null,
}));

describe("Header", () => {
    let hostPath = process.env.HOST;
    let wrapper: RenderResult;
    let history = createBrowserHistory();
    pluginRegistry.registerPluginComponent<BrandingProps>(SUPPORTED_PLUGINS.DI_BRANDING, {
        applicationCorporationName: "some corp",
        applicationName: "some app",
        applicationSuiteName: "some suite",
    });

    beforeEach(() => {
        // add explicitely extra tragets for portals, @see https://stackoverflow.com/a/48094582
        const portalroot = global.document.createElement("div");
        portalroot.setAttribute("id", APP_VIEWHEADER_ID);
        global.document.querySelector("body").appendChild(portalroot);
        history.push(workspacePath("/projects/SomeProjectId/dataset/SomeTaskId"));

        setUseParams("SomeProjectId", "SomeTaskId");

        wrapper = renderWrapper(
            <Header onClickApplicationSidebarExpand={() => {}} isApplicationSidebarExpanded={false} />,
            history,
            {
                common: { initialSettings: { dmBaseUrl: "http://docker.local" } },
            },
        );
    });

    afterEach(() => {
        wrapper.unmount();
        mockAxios.reset();
    });

    it("exposes the fallback Data Manager entry as a link", () => {
        const sidebar = wrapper.container.querySelector(".eccgui-application__menu__sidebar") as HTMLElement;
        expect(sidebar.querySelectorAll('[role="list"]')).toHaveLength(2);
        expect(sidebar.querySelectorAll('[role="menu"], [role="menuitem"]')).toHaveLength(0);
        expect(sidebar.querySelector('a[href="http://docker.local"]')).not.toBeNull();
    });

    it("exposes sidebar navigation entries as links in lists", () => {
        wrapper.unmount();
        wrapper = renderWrapper(
            <Header onClickApplicationSidebarExpand={() => {}} isApplicationSidebarExpanded={true} />,
            history,
            {
                common: {
                    initialSettings: {
                        dmBaseUrl: "http://docker.local",
                        dmModuleLinks: [{ path: "thesaurus", defaultLabel: "Thesauri" }],
                    },
                },
            },
        );

        const sidebar = wrapper.container.querySelector(".eccgui-application__menu__sidebar") as HTMLElement;
        expect(sidebar).not.toBeNull();
        expect(sidebar.querySelectorAll('[role="list"]')).toHaveLength(2);
        expect(sidebar.querySelectorAll('[role="menu"], [role="menuitem"]')).toHaveLength(0);
        expect(sidebar.querySelectorAll("a[href]")).toHaveLength(5);
        expect(sidebar.querySelector('a[href="http://docker.local/thesaurus"]')).not.toBeNull();
    });

    it("exposes the user panel as a list of links, not a menu", () => {
        fireEvent.click(wrapper.container.querySelector("#headerUserMenu") as HTMLElement);
        const panel = document.querySelector('[aria-label="User menu"]') as HTMLElement;
        expect(panel).not.toBeNull();
        expect(panel.querySelector('[role="list"]')).not.toBeNull();
        expect(panel.querySelector('[role="menu"], [role="menuitem"]')).toBeNull();
    });

    it("should page title is correct", () => {
        wrapper = renderWrapper(
            <PageHeader
                pageTitle="My Page Title"
                type="artefacttype"
                breadcrumbs={[
                    { href: "/workbench", text: "Workbench" },
                    { href: "/workbench/projects/SomeProjectId", text: "My Project" },
                    {
                        href: "/workbench/projects/SomeProjectId/transform/SomeTransformId",
                        text: "My Transform Title",
                    },
                ]}
            />,
            history,
            {
                common: { initialSettings: { dmBaseUrl: "http://docker.local" } },
            },
        );
        const helmet = Helmet.peek();
        expect(helmet.title).toBe("My Page Title (artefacttype) at Workbench / My Project — some corp some suite");
    });

    it("should delete button works properly", async () => {
        wrapper = renderWrapper(<Task />, history, {
            common: { initialSettings: { dmBaseUrl: "http://docker.local" } },
        });
        const removeHeaderButton = document.querySelector(byTestId("header-remove-button")) as Element;
        fireEvent.click(removeHeaderButton);
        const removeItemButton = document.querySelector(byTestId("remove-item-button")) as Element;
        fireEvent.click(removeItemButton);
        mockAxios.mockResponseFor(
            {
                url: hostPath + "/workspace/projects/SomeProjectId/tasks/SomeTaskId",
                method: "DELETE",
            },
            mockedAxiosResponse(),
        );

        await waitFor(() => {
            expect(window.location.pathname).toBe(workspacePath("/projects/SomeProjectId"));
        });
    });
});
