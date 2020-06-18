import React from "react";
import { createBrowserHistory } from "history";
import mockAxios from "../../../../__mocks__/axios";
import { clickElement, mockedAxiosResponse, testWrapper, withWindowLocation } from "../../../TestHelper";
import { Header } from "../../../../../src/app/views/layout/Header/Header";
import { waitFor } from "@testing-library/react";

describe("Header", () => {
    let hostPath = process.env.HOST;
    let wrapper;

    beforeEach(() => {
        const history = createBrowserHistory();
        history.location.pathname = "/workspace-beta";

        wrapper = testWrapper(<Header />, history);
    });

    afterEach(() => {
        mockAxios.reset();
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
