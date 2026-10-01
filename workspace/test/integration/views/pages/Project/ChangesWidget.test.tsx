import React from "react";
import "@testing-library/jest-dom";
import { act, waitFor } from "@testing-library/react";
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

    const renderWidget = (unreviewed: number, history = createBrowserHistory()) => {
        // The project id is taken from the URL by the router
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

    it("should refetch the count when the tab comes back into view", async () => {
        const wrapper = renderWidget(0);
        await waitFor(() => {
            expect(wrapper.container.querySelector("h2")?.textContent).toBe("Changes");
        });
        // The tab is shown again (jsdom keeps it visible), so a change made meanwhile is picked up
        act(() => {
            document.dispatchEvent(new Event("visibilitychange"));
        });
        mockAxios.mockResponseFor(
            { url: summaryUrl },
            mockedAxiosResponse({ data: { reviewedUpTo: 2, latestSeq: 6, unreviewed: 1 } }),
        );
        await waitFor(() => {
            expect(wrapper.container.querySelector("h2")?.textContent).toBe("Changes (1 unreviewed)");
        });
    });

    it("should refetch the count once when the window regains focus, also if the tab is shown at the same time", async () => {
        const wrapper = renderWidget(0);
        await waitFor(() => {
            expect(wrapper.container.querySelector("h2")?.textContent).toBe("Changes");
        });
        // Coming back from another application fires focus, from a hidden tab both events; one request either way
        act(() => {
            window.dispatchEvent(new Event("focus"));
            document.dispatchEvent(new Event("visibilitychange"));
        });
        expect(mockAxios.queue()).toHaveLength(1);
        mockAxios.mockResponseFor(
            { url: summaryUrl },
            mockedAxiosResponse({ data: { reviewedUpTo: 2, latestSeq: 6, unreviewed: 2 } }),
        );
        await waitFor(() => {
            expect(wrapper.container.querySelector("h2")?.textContent).toBe("Changes (2 unreviewed)");
        });
    });

    it("should drop the count of the previous project when switching to another", async () => {
        const history = createBrowserHistory();
        const wrapper = renderWidget(3, history);
        await waitFor(() => {
            expect(wrapper.container.querySelector("h2")?.textContent).toBe("Changes (3 unreviewed)");
        });
        // The other project's count is unknown until it answers, and stays unknown when the request fails
        act(() => history.push(workspacePath("/projects/otherProject")));
        await waitFor(() => {
            expect(wrapper.container.querySelector("h2")?.textContent).toBe("Changes");
        });
        mockAxios.mockError(new Error("forbidden"), mockAxios.getReqByUrl(apiUrl("/workspace/projects/otherProject/changes/summary")));
        await waitFor(() => {
            expect(mockAxios.queue()).toHaveLength(0);
        });
        expect(wrapper.container.querySelector("h2")?.textContent).toBe("Changes");
    });

    it("should show the plain title when nothing is unreviewed", async () => {
        const wrapper = renderWidget(0);
        await waitFor(() => {
            expect(wrapper.container.querySelector(byTestId("open-project-changes-btn"))).toBeInTheDocument();
        });
        expect(wrapper.container.querySelector("h2")?.textContent).toBe("Changes");
    });
});
