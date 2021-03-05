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
} from "../../../TestHelper";
import { Header } from "../../../../../src/app/views/layout/Header/Header";
import { APP_VIEWHEADER_ID } from "../../../../../src/app/views/layout/Header/ViewHeader";
import { ViewHeaderContentProvider } from "../../../../../src/app/views/layout/Header/ViewHeaderContentProvider";
import { waitFor } from "@testing-library/react";
import { Helmet } from "react-helmet";
import { ContextMenu } from "../../../../../src/libs/gui-elements";

describe("Header", () => {
    let hostPath = process.env.HOST;
    let wrapper;
    let history = createBrowserHistory();

    beforeEach(() => {
        history.location.pathname = workspacePath("/projects/SomeProjectId/dataset/SomeTaskId");

        wrapper = withMount(
            testWrapper(<Header />, history, {
                common: { initialSettings: { dmBaseUrl: "http://docker.local" } },
            })
        );
    });

    afterEach(() => {
        mockAxios.reset();
    });

    // TODO: reactivate, probably problem cause by portal usage
    xit("should page title is correct", () => {
        const history = createBrowserHistory();
        history.location.pathname = workspacePath("/projects/SomeProjectId/dataset/SomeTaskId");

        wrapper = withMount(
            testWrapper(
                <div id={APP_VIEWHEADER_ID}>
                    <ViewHeaderContentProvider breadcrumbs={[{ href: "/someHref", text: "dummy bread" }]} />
                </div>,
                history,
                {
                    common: { initialSettings: { dmBaseUrl: "http://docker.local" } },
                }
            )
        );

        expect(wrapper.find(Helmet).prop("title")).toEqual("dummy bread (dataset) – eccenca Corporate Memory");
    });

    // TODO: reactivate, probably problem cause by portal usage
    xit("should delete button works properly", async () => {
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
        wrapper.find(ContextMenu).simulate("click");
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

    it("should have a logout action that triggers a logout", async () => {
        await withWindowLocation(async () => {
            clickElement(wrapper, "#headerUserMenu");
            clickElement(wrapper, "#logoutAction");
            await waitFor(() => {
                expect(window.location.pathname).toBe("/logout");
            });
        });
    });
});
