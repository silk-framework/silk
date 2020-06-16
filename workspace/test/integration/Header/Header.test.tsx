import React from "react";
import "@testing-library/jest-dom";
import { createBrowserHistory } from "history";
import mockAxios from "../../__mocks__/axios";
import { clickElement, logRequests, testWrapper } from "../TestHelper";
import { Header } from "../../../src/app/views/layout/Header/Header";

describe("Header", () => {
    let hostPath = process.env.HOST;
    afterEach(() => {
        mockAxios.reset();
    });

    const loadHeader = () => {
        const history = createBrowserHistory();
        history.location.pathname = "/workspace-beta";
        const header = <Header />;

        return testWrapper(header, history);
    };

    it("should have a logout action that triggers a logout", async () => {
        const wrapper = loadHeader();
        clickElement(wrapper, "button#headerUserMenu");
        clickElement(wrapper, "a#logoutAction");
        const reqInfo = mockAxios.getReqMatching({
            url: hostPath + "/logout",
            method: "POST",
        });
        expect(reqInfo).toBeTruthy();
    });
});
