import React from "react";
import "@testing-library/jest-dom";
import { createBrowserHistory } from "history";
import mockAxios from "../../../../__mocks__/axios";
import { clickElement, eventually, mockAxiosResponse, testWrapper, withWindowLocation } from "../../../TestHelper";
import { Header } from "../../../../../src/app/views/layout/Header/Header";

describe("Header", () => {
    let hostPath = process.env.HOST;
    afterEach(() => {
        mockAxios.reset();
    });

    const loadHeader = () => {
        const history = createBrowserHistory();
        history.location.pathname = "/workspace-beta";

        return testWrapper(<Header />, history);
    };

    it("should have a logout action that triggers a logout", async () => {
        const wrapper = loadHeader();
        await withWindowLocation(async () => {
            clickElement(wrapper, "#headerUserMenu");
            clickElement(wrapper, "#logoutAction");
            mockAxios.mockResponseFor({ url: hostPath + "/logout", method: "POST" }, mockAxiosResponse());
            await eventually(() => {
                expect(window.location.pathname).toBe("/loggedOut");
            });
        });
    });
});
