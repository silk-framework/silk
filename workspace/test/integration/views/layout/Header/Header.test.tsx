import React from "react";
import { createBrowserHistory } from "history";
import mockAxios from "../../../../__mocks__/axios";
import {
    byTestId,
    clickElement,
    mockedAxiosResponse,
    testWrapper,
    withMount,
    withWindowLocation,
    workspacePath,
    logRequests,
    setUseParams,
} from "../../../TestHelper";
import { Header } from "../../../../../src/app/views/layout/Header/Header";
import Task from "../../../../../src/app/views/pages/Task";
import { PageHeader, APP_VIEWHEADER_ID } from "../../../../../src/app/views/shared/PageHeader/PageHeader";
import { waitFor } from "@testing-library/react";
import { Helmet } from "react-helmet";
import { ContextMenu } from "gui-elements";

describe("Header", () => {
    let hostPath = process.env.HOST;
    let wrapper;
    let history = createBrowserHistory();

    beforeEach(() => {
        // add explicitely extra tragets for portals, @see https://stackoverflow.com/a/48094582
        const portalroot = global.document.createElement("div");
        portalroot.setAttribute("id", APP_VIEWHEADER_ID);
        global.document.querySelector("body").appendChild(portalroot);

        history.location.pathname = workspacePath("/projects/SomeProjectId/dataset/SomeTaskId");

        setUseParams("SomeProjectId", "SomeTaskId");

        wrapper = withMount(
            testWrapper(<Header />, history, {
                common: { initialSettings: { dmBaseUrl: "http://docker.local" } },
            })
        );
    });

    afterEach(() => {
        wrapper.unmount();
        mockAxios.reset();
    });

    it("should page title is correct", () => {
        wrapper = withMount(
            testWrapper(
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
                }
            )
        );

        expect(wrapper.find(Helmet).prop("title")).toEqual(
            "My Page Title (artefacttype) at Workbench / My Project — eccenca Corporate Memory"
        );
    });

    it("should delete button works properly", async () => {
        wrapper = withMount(
            testWrapper(<Task />, history, {
                common: { initialSettings: { dmBaseUrl: "http://docker.local" } },
            })
        );

        clickElement(wrapper, byTestId("header-remove-button"));
        clickElement(wrapper, byTestId("remove-item-button"));
        mockAxios.mockResponseFor(
            {
                url: hostPath + "/workspace/projects/SomeProjectId/tasks/SomeTaskId",
                method: "DELETE",
            },
            mockedAxiosResponse()
        );

        await waitFor(() => {
            expect(window.location.pathname).toBe("/");
        });
    });

    xit("should clone button works properly", async () => {
        wrapper = withMount(
            testWrapper(<Task />, history, {
                common: { initialSettings: { dmBaseUrl: "http://docker.local" } },
            })
        );

        wrapper.find(ContextMenu).simulate("click");
        clickElement(wrapper, byTestId("header-clone-button"));
        clickElement(wrapper, byTestId("clone-modal-button"));

        mockAxios.mockResponseFor(
            {
                url: hostPath + "/workspace/projects/SomeProjectId/tasks/SomeTaskId/clone",
                method: "POST",
            },
            mockedAxiosResponse({
                data: {
                    detailsPage: workspacePath("/projects/SomeProjectId/dataset/SomeTaskId"),
                },
            })
        );

        await waitFor(() => {
            expect(window.location.pathname).toBe(workspacePath("/projects/SomeProjectId/dataset/SomeTaskId"));
        });
    });

    // TODO: Move test to plugin code
    // it("should have a logout action that triggers a logout", async () => {
    //     await withWindowLocation(async () => {
    //         clickElement(wrapper, "#headerUserMenu");
    //         clickElement(wrapper, "#logoutAction");
    //         await waitFor(() => {
    //             expect(window.location.pathname).toBe("/logout");
    //         });
    //     });
    // });
});
