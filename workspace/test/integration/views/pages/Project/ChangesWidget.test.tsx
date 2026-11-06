import React from "react";
import "@testing-library/jest-dom";
import { act, RenderResult, waitFor } from "@testing-library/react";
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

    const answerSummary = (unreviewed: number, latestSeq: number = 5, droppedUnreviewed: number = 0) =>
        mockAxios.mockResponseFor(
            { url: summaryUrl },
            mockedAxiosResponse({
                data: { reviewedUpTo: latestSeq - unreviewed, latestSeq, unreviewed, droppedUnreviewed },
            }),
        );

    const renderWidget = (
        unreviewed: number,
        history = createBrowserHistory(),
        latestSeq: number = 5,
        droppedUnreviewed: number = 0,
    ) => {
        // The project id is taken from the URL by the router
        history.location.pathname = workspacePath(`/projects/${PROJECT_ID}`);
        const wrapper = renderWrapper(<ChangesWidget />, history);
        answerSummary(unreviewed, latestSeq, droppedUnreviewed);
        return wrapper;
    };

    /** The status line of the widget, or null while the journal's numbers are unknown. */
    const status = (wrapper: RenderResult) =>
        wrapper.container.querySelector(byTestId("project-changes-status"))?.textContent ?? null;

    it("should tell how many changes await review and offer to review them", async () => {
        const wrapper = renderWidget(3);
        await waitFor(() => {
            expect(status(wrapper)).toContain("Changes made by an agent awaiting your review: 3");
        });
        expect(wrapper.container.querySelector(byTestId("review-project-changes-btn"))).toBeInTheDocument();
        expect(wrapper.container.querySelector(byTestId("open-project-changes-btn"))).toBeInTheDocument();
    });

    it("should say what has been recorded when nothing awaits review", async () => {
        const wrapper = renderWidget(0);
        await waitFor(() => {
            expect(status(wrapper)).toBe("Recorded changes: 5. Nothing awaits your review.");
        });
        expect(wrapper.container.querySelector(byTestId("review-project-changes-btn"))).toBeNull();
    });

    it("should ask for the review when the journal dropped unreviewed changes, although none is left to show", async () => {
        const wrapper = renderWidget(0, createBrowserHistory(), 5, 2);
        await waitFor(() => {
            expect(status(wrapper)).toContain(
                "Changes made by an agent that the journal dropped before your review: 2",
            );
        });
        expect(wrapper.container.querySelector(byTestId("review-project-changes-btn"))).toBeInTheDocument();
    });

    it("should name the dropped changes next to those awaiting review", async () => {
        const wrapper = renderWidget(1, createBrowserHistory(), 5, 2);
        await waitFor(() => {
            expect(status(wrapper)).toContain("Changes made by an agent awaiting your review: 1");
        });
        expect(status(wrapper)).toContain("Changes made by an agent that the journal dropped before your review: 2");
    });

    it("should say so when no change has been recorded", async () => {
        const wrapper = renderWidget(0, createBrowserHistory(), 0);
        await waitFor(() => {
            expect(status(wrapper)).toBe("No changes recorded yet.");
        });
    });

    it("should refetch the numbers when the tab comes back into view", async () => {
        const wrapper = renderWidget(0);
        await waitFor(() => {
            expect(status(wrapper)).toBe("Recorded changes: 5. Nothing awaits your review.");
        });
        // The tab is shown again (jsdom keeps it visible), so a change made meanwhile is picked up
        act(() => {
            document.dispatchEvent(new Event("visibilitychange"));
        });
        answerSummary(1, 6);
        await waitFor(() => {
            expect(status(wrapper)).toContain("Changes made by an agent awaiting your review: 1");
        });
    });

    it("should refetch the numbers once when the window regains focus, also if the tab is shown at the same time", async () => {
        const wrapper = renderWidget(0);
        await waitFor(() => {
            expect(status(wrapper)).toBe("Recorded changes: 5. Nothing awaits your review.");
        });
        // Coming back from another application fires focus, from a hidden tab both events; one request either way
        act(() => {
            window.dispatchEvent(new Event("focus"));
            document.dispatchEvent(new Event("visibilitychange"));
        });
        expect(mockAxios.queue()).toHaveLength(1);
        answerSummary(2, 6);
        await waitFor(() => {
            expect(status(wrapper)).toContain("Changes made by an agent awaiting your review: 2");
        });
    });

    it("should drop the numbers of the previous project when switching to another", async () => {
        const history = createBrowserHistory();
        const wrapper = renderWidget(3, history);
        await waitFor(() => {
            expect(status(wrapper)).toContain("Changes made by an agent awaiting your review: 3");
        });
        // The other project's numbers are unknown until it answers, and stay unknown when the request fails
        act(() => history.push(workspacePath("/projects/otherProject")));
        await waitFor(() => {
            expect(status(wrapper)).toBeNull();
        });
        mockAxios.mockError(
            new Error("forbidden"),
            mockAxios.getReqByUrl(apiUrl("/workspace/projects/otherProject/changes/summary")),
        );
        await waitFor(() => {
            expect(mockAxios.queue()).toHaveLength(0);
        });
        expect(status(wrapper)).toBeNull();
    });
});
