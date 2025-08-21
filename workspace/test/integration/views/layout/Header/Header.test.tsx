import React from "react";
import { createBrowserHistory, createMemoryHistory } from "history";
import mockAxios from "../../../../__mocks__/axios";
import {
    byTestId,
    clickFoundElement,
    findElement,
    mockedAxiosResponse,
    renderWrapper,
    setUseParams,
    testWrapper,
    withMount,
    workspacePath,
} from "../../../TestHelper";
import { Header } from "../../../../../src/app/views/layout/Header/Header";
import Task from "../../../../../src/app/views/pages/Task";
import { APP_VIEWHEADER_ID, PageHeader } from "../../../../../src/app/views/shared/PageHeader/PageHeader";
import { fireEvent, RenderResult, waitFor } from "@testing-library/react";
import { Helmet } from "react-helmet";
import { pluginRegistry, SUPPORTED_PLUGINS } from "../../../../../src/app/views/plugins/PluginRegistry";
import { BrandingProps } from "../../../../../src/app/views/plugins/plugin.types";

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

    xit("should clone button works properly", async () => {
        wrapper = renderWrapper(<Task />, history, {
            common: { initialSettings: { dmBaseUrl: "http://docker.local" } },
        });

        fireEvent.click(findElement(wrapper, "[class*='contextmenu']"));
        clickFoundElement(wrapper, byTestId("header-clone-button"));
        clickFoundElement(wrapper, byTestId("clone-modal-button"));

        mockAxios.mockResponseFor(
            {
                url: hostPath + "/workspace/projects/SomeProjectId/tasks/SomeTaskId/clone",
                method: "POST",
            },
            mockedAxiosResponse({
                data: {
                    detailsPage: workspacePath("/projects/SomeProjectId/dataset/SomeTaskId"),
                },
            }),
        );

        await waitFor(() => {
            expect(window.location.pathname).toBe(workspacePath("/projects/SomeProjectId/dataset/SomeTaskId"));
        });
    });
});
