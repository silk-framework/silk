import React from "react";
import "@testing-library/jest-dom";
import { RenderResult, waitFor } from "@testing-library/react";
import mockAxios from "../../../../__mocks__/axios";
import {
    apiUrl,
    byTestId,
    checkRequestMade,
    clickFoundElement,
    findElement,
    mockedAxiosResponse,
    renderWrapper,
} from "../../../TestHelper";
import ChangeList from "../../../../../src/app/views/pages/ProjectChanges/ChangeList";
import { IChangeEntry } from "../../../../../src/app/views/pages/ProjectChanges/changesRequests";

describe("Project changes", () => {
    afterEach(() => {
        mockAxios.reset();
    });

    const PROJECT_ID = "cmem";
    const changesUrl = apiUrl(`/workspace/projects/${PROJECT_ID}/changes`);
    const revertUrl = (seq: number) => apiUrl(`/workspace/projects/${PROJECT_ID}/changes/${seq}/revert`);
    const revertAllUrl = apiUrl(`/workspace/projects/${PROJECT_ID}/changes/revert`);
    const reviewedUrl = apiUrl(`/workspace/projects/${PROJECT_ID}/changes/reviewed`);

    const changes: IChangeEntry[] = [
        {
            seq: 3,
            timestamp: "2026-08-26T09:51:02.417Z",
            user: "urn:user:alice",
            origin: "mcp:claude-code",
            type: "AddMapping",
            description: "Added mapping rule 'name' under 'root' in transform 'persons'",
            revertible: true,
            unreviewed: true,
        },
        {
            // A reverted entry is never flagged unreviewed
            seq: 2,
            timestamp: "2026-08-26T09:50:12.345Z",
            user: "urn:user:alice",
            origin: "mcp:claude-code",
            type: "ReplaceTask",
            description: "Updated task 'persons'",
            revertible: true,
            revertedBy: 3,
        },
        {
            seq: 1,
            timestamp: "2026-08-26T09:49:58.001Z",
            user: "urn:user:alice",
            origin: "mcp:claude-code",
            type: "WorkflowExecuted",
            description: "Executed workflow 'workflow'",
            revertible: false,
            unreviewed: true,
        },
    ];

    const reviewedChanges = changes.map(({ unreviewed, ...change }) => change);

    const loadChangeList = async (): Promise<RenderResult> => {
        const wrapper = renderWrapper(<ChangeList projectId={PROJECT_ID} />);
        mockAxios.mockResponseFor({ url: changesUrl }, mockedAxiosResponse({ data: { reviewedUpTo: 0, changes } }));
        await waitFor(() => {
            expect(wrapper.container.querySelectorAll("tbody tr")).toHaveLength(changes.length);
        });
        return wrapper;
    };

    it("should list all changes with their descriptions", async () => {
        const wrapper = await loadChangeList();
        changes.forEach((change) => {
            expect(wrapper.container.textContent).toContain(change.description);
        });
        expect(wrapper.container.textContent).toContain("mcp:claude-code");
    });

    it("should only offer to revert changes that are revertible and not reverted already", async () => {
        const wrapper = await loadChangeList();
        expect(findElement(wrapper, byTestId("change-revert-btn-3"))).not.toBeDisabled();
        expect(findElement(wrapper, byTestId("change-revert-btn-2"))).toBeDisabled();
        expect(findElement(wrapper, byTestId("change-revert-btn-1"))).toBeDisabled();
    });

    it("should revert a change after confirmation and reload the list", async () => {
        const wrapper = await loadChangeList();
        clickFoundElement(wrapper, byTestId("change-revert-btn-3"));
        await waitFor(() => {
            expect(findElement(document.body, byTestId("remove-item-button"))).toBeInTheDocument();
        });
        clickFoundElement(document.body, byTestId("remove-item-button"));
        await waitFor(() => {
            checkRequestMade(revertUrl(3), "POST");
        });
        mockAxios.mockResponseFor(
            { url: revertUrl(3) },
            mockedAxiosResponse({
                data: {
                    seq: 4,
                    timestamp: "2026-08-26T09:52:00.000Z",
                    type: "RemoveMapping",
                    revertible: true,
                    reverts: 3,
                },
            }),
        );
        // The list is reloaded after the revert
        await waitFor(() => {
            expect(mockAxios.queue().length).toBeGreaterThan(0);
        });
        mockAxios.mockResponseFor({ url: changesUrl }, mockedAxiosResponse({ data: { reviewedUpTo: 0, changes } }));
        await waitFor(() => {
            expect(document.body.querySelector(byTestId("remove-item-button"))).not.toBeInTheDocument();
        });
    });

    it("should mark the unreviewed changes and offer the review actions", async () => {
        const wrapper = await loadChangeList();
        expect(wrapper.container.textContent).toContain("Unreviewed changes: 2");
        expect(findElement(wrapper, byTestId("changes-mark-reviewed-btn"))).toBeInTheDocument();
        expect(findElement(wrapper, byTestId("changes-revert-unreviewed-btn"))).toBeInTheDocument();
    });

    it("should mark all changes as reviewed with the latest fetched seq", async () => {
        const wrapper = await loadChangeList();
        clickFoundElement(wrapper, byTestId("changes-mark-reviewed-btn"));
        await waitFor(() => {
            expect(findElement(document.body, byTestId("changes-mark-reviewed-confirm-btn"))).toBeInTheDocument();
        });
        clickFoundElement(document.body, byTestId("changes-mark-reviewed-confirm-btn"));
        await waitFor(() => {
            checkRequestMade(reviewedUrl, "PUT", { upTo: 3 });
        });
        mockAxios.mockResponseFor({ url: reviewedUrl }, mockedAxiosResponse({ data: { reviewedUpTo: 3 } }));
        await waitFor(() => {
            expect(mockAxios.queue().length).toBeGreaterThan(0);
        });
        mockAxios.mockResponseFor(
            { url: changesUrl },
            mockedAxiosResponse({ data: { reviewedUpTo: 3, changes: reviewedChanges } }),
        );
        await waitFor(() => {
            expect(wrapper.container.querySelector(byTestId("changes-mark-reviewed-btn"))).not.toBeInTheDocument();
        });
    });

    it("should revert the unreviewed changes and report the outcome", async () => {
        const wrapper = await loadChangeList();
        clickFoundElement(wrapper, byTestId("changes-revert-unreviewed-btn"));
        await waitFor(() => {
            expect(findElement(document.body, byTestId("remove-item-button"))).toBeInTheDocument();
        });
        // The dialog lists what will be attempted and notes the non-revertible entry that will be skipped
        expect(document.body.textContent).toContain(changes[0].description);
        expect(document.body.textContent).toContain("Skipped as not revertible: 1.");
        clickFoundElement(document.body, byTestId("remove-item-button"));
        await waitFor(() => {
            checkRequestMade(revertAllUrl, "POST", { seqs: [3, 1] });
        });
        mockAxios.mockResponseFor(
            { url: revertAllUrl },
            mockedAxiosResponse({
                data: {
                    results: [
                        { seq: 3, outcome: "reverted", entry: { seq: 4, type: "RemoveMapping", revertible: true } },
                        { seq: 1, outcome: "skipped", message: "Change 1 (WorkflowExecuted) cannot be reverted." },
                    ],
                },
            }),
        );
        await waitFor(() => {
            expect(mockAxios.queue().length).toBeGreaterThan(0);
        });
        mockAxios.mockResponseFor({ url: changesUrl }, mockedAxiosResponse({ data: { reviewedUpTo: 0, changes } }));
        await waitFor(() => {
            const summary = findElement(wrapper, byTestId("changes-revert-all-summary"));
            expect(summary.textContent).toContain("Reverted changes: 1.");
            expect(summary.textContent).toContain("Skipped: 1.");
        });
    });
});
