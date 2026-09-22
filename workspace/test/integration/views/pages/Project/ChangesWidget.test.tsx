import React from "react";
import "@testing-library/jest-dom";
import { waitFor } from "@testing-library/react";
import mockAxios from "../../../../__mocks__/axios";
import { apiUrl, byTestId, mockedAxiosResponse, renderWrapper, workspacePath } from "../../../TestHelper";
import { createBrowserHistory } from "history";
import ChangesWidget from "../../../../../src/app/views/pages/Project/ChangesWidget";

describe("Changes widget", () => {
    const PROJECT_ID = "testproject";
    const summaryUrl = apiUrl(`/workspace/projects/${PROJECT_ID}/changes/summary`);

    afterEach(() => {
        mockAxios.reset();
    });

    const renderWidget = (unreviewed: number) => {
        // The project id is taken from the URL by the router
        const history = createBrowserHistory();
        history.location.pathname = workspacePath(`/projects/${PROJECT_ID}`);
        const wrapper = renderWrapper(<ChangesWidget />, history);
        mockAxios.mockResponseFor(
            { url: summaryUrl },
            mockedAxiosResponse({ data: { reviewedUpTo: 2, latestSeq: 5, unreviewed } }),
        );
        return wrapper;
    };

    it("should count the unreviewed changes in the title", async () => {
        const wrapper = renderWidget(3);
        await waitFor(() => {
            expect(wrapper.container.querySelector("h2")?.textContent).toBe("Changes (3 unreviewed)");
        });
        expect(wrapper.container.querySelector(byTestId("open-project-changes-btn"))).toBeInTheDocument();
    });

    it("should show the plain title when nothing is unreviewed", async () => {
        const wrapper = renderWidget(0);
        await waitFor(() => {
            expect(wrapper.container.querySelector(byTestId("open-project-changes-btn"))).toBeInTheDocument();
        });
        expect(wrapper.container.querySelector("h2")?.textContent).toBe("Changes");
    });
});
