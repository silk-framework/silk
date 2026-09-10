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
import {
    BrandingProps,
    CompanionToolbarProps,
} from "../../../../../src/app/views/plugins/plugin.types";

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
    pluginRegistry.registerReactPluginComponent<CompanionToolbarProps>({
        id: SUPPORTED_PLUGINS.DI_COMPANION,
        label: "Companion test plugin",
        Component: ({ companionConfig }) => (
            <div data-test-id="companion-plugin">{companionConfig.apiBasePath}</div>
        ),
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

    it("should only mount the companion plugin when enabled for the current user", () => {
        expect(wrapper.container.querySelector(byTestId("companion-plugin"))).not.toBeInTheDocument();

        wrapper.unmount();
        wrapper = renderWrapper(
            <Header onClickApplicationSidebarExpand={() => {}} isApplicationSidebarExpanded={false} />,
            history,
            {
                common: {
                    initialSettings: {
                        companion: {
                            enabled: true,
                            apiBasePath: "/explore/api/companion",
                            streamPath: "/explore/companion-websocket",
                        },
                    },
                },
            },
        );

        expect(wrapper.container.querySelector(byTestId("companion-plugin"))).toHaveTextContent(
            "/explore/api/companion",
        );
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
