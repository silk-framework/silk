import React from "react";
import { createBrowserHistory } from "history";
import mockAxios from "../../../../__mocks__/axios";
import { clickElement, mockedAxiosResponse, testWrapper, withWindowLocation } from "../TestHelper";
import { Header } from "../../../../../src/app/views/layout/Header/Header";
import { waitFor } from "@testing-library/react";
import { Helmet } from "react-helmet";
import { ContextMenu, MenuItem } from "../../../src/app/wrappers";

describe("Header", () => {
    let hostPath = process.env.HOST;
    let wrapper;

    beforeEach(() => {
        const history = createBrowserHistory();
        history.location.pathname = "/workspace-beta/projects/SomeProjectId/dataset/SomeTaskId";

        wrapper = testWrapper(<Header breadcrumbs={[{ href: "/someHref", text: "dummy bread" }]} />, history);
    });

    afterEach(() => {
        mockAxios.reset();
    });

    it("should page title is correct", () => {
        expect(wrapper.find(Helmet).prop("title")).toEqual("dummy bread (dataset) at  – eccenca Corporate Memory");
    });

    it("should delete button works properly", async () => {
        clickElement(wrapper, '[data-test-id="header-remove-button"]');
        clickElement(wrapper, '[data-test-id="remove-item-button"]');
        mockAxios.mockResponseFor(
            {
                url: hostPath + "/workspace/projects/SomeProjectId/tasks/SomeTaskId?removeDependentTasks=true",
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
        clickElement(wrapper, '[data-test-id="clone-modal-button"]');

        mockAxios.mockResponseFor(
            {
                url: hostPath + "/workspace/projects/SomeProjectId/tasks/SomeTaskId/clone",
                method: "POST",
            },
            mockedAxiosResponse({
                data: {
                    detailsPage: "/workspace-beta/projects/SomeProjectId/dataset/SomeTaskId",
                },
            })
        );

        await waitFor(() => {
            expect(window.location.pathname).toBe("/workspace-beta/projects/SomeProjectId/dataset/SomeTaskId");
        });
    });

    it("should have a logout action that triggers a logout", async () => {
        await withWindowLocation(async () => {
            clickElement(wrapper, "#headerUserMenu");
            clickElement(wrapper, "#logoutAction");
            mockAxios.mockResponseFor({ url: hostPath + "/logout", method: "POST" }, mockedAxiosResponse());
            await waitFor(() => {
                expect(window.location.pathname).toBe("/loggedOut");
            });
        });
    });
});
