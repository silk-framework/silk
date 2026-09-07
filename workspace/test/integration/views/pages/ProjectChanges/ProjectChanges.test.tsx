import React from "react";
import "@testing-library/jest-dom";
import { fireEvent, RenderResult, waitFor } from "@testing-library/react";
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

    const transformLink = {
        id: "details",
        label: "Transform details page",
        path: `/workbench/projects/${PROJECT_ID}/transform/persons`,
    };
    // Newest first: an agent mapping, the user revert of the update, the reverted update, a run
    const changes: IChangeEntry[] = [
        {
            seq: 4,
            timestamp: "2026-08-26T09:51:02.417Z",
            user: "urn:user:alice",
            origin: "mcp:claude-code",
            type: "AddMapping",
            description:
                "Added value mapping 'name' (name → http://xmlns.com/foaf/0.1/name) under 'root' in transform 'persons'",
            summary:
                "Added value mapping 'name' (name → http://xmlns.com/foaf/0.1/name) under 'root' in transform 'persons'",
            details: [],
            links: [{ id: "rule", label: "Mapping rule 'name'", path: `${transformLink.path}?ruleId=name` }],
            revertible: true,
            unreviewed: true,
        },
        {
            seq: 3,
            timestamp: "2026-08-26T09:50:40.000Z",
            user: "urn:user:alice",
            type: "ReplaceTask",
            description: "Updated transform 'persons': Output dataset 'out' → ''",
            summary: "Updated transform 'persons'",
            details: [{ label: "Output dataset", before: "out", after: "" }],
            links: [transformLink],
            revertible: true,
            reverts: 2,
        },
        {
            // A reverted entry is never flagged unreviewed
            seq: 2,
            timestamp: "2026-08-26T09:50:12.345Z",
            user: "urn:user:alice",
            origin: "mcp:claude-code",
            type: "ReplaceTask",
            description: "Updated transform 'persons': Output dataset '' → 'out', Mapping rule changed",
            summary: "Updated transform 'persons'",
            details: [{ label: "Output dataset", before: "", after: "out" }, { label: "Mapping rule changed" }],
            links: [transformLink],
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
            summary: "Executed workflow 'workflow'",
            details: [],
            links: [
                {
                    id: "details",
                    label: "Workflow details page",
                    path: `/workbench/projects/${PROJECT_ID}/workflow/workflow`,
                },
                {
                    id: "report",
                    label: "Execution report",
                    path: `/api/workspace/reports/report?projectId=${PROJECT_ID}&taskId=workflow&time=2026-08-26T09:49:55.000Z`,
                    openInNewTab: true,
                },
            ],
            revertible: false,
            unreviewed: true,
        },
    ];
    const [mappingChange, revertChange] = changes;

    const reviewedChanges = changes.map(({ unreviewed, ...change }) => change);

    const loadChangeList = async (): Promise<RenderResult> => {
        const wrapper = renderWrapper(<ChangeList projectId={PROJECT_ID} />);
        mockAxios.mockResponseFor({ url: changesUrl }, mockedAxiosResponse({ data: { reviewedUpTo: 0, changes } }));
        await waitFor(() => {
            expect(wrapper.container.querySelectorAll("tbody tr")).toHaveLength(changes.length);
        });
        return wrapper;
    };

    /** Opens the actions menu of a change and returns the menu item with the given test id. */
    const openMenuItem = async (wrapper: RenderResult, seq: number, itemTestId: string): Promise<Element> => {
        clickFoundElement(wrapper, byTestId(`change-menu-${seq}`));
        await waitFor(() => {
            expect(findElement(document.body, byTestId(itemTestId))).toBeInTheDocument();
        });
        return findElement(document.body, byTestId(itemTestId));
    };

    const isDisabled = (menuItem: Element): boolean => menuItem.getAttribute("aria-disabled") === "true";

    it("should list all changes with their summaries and details", async () => {
        const wrapper = await loadChangeList();
        changes.forEach((change) => {
            expect(wrapper.container.textContent).toContain(change.summary);
        });
        // An agent change is marked by an icon carrying the origin
        expect(findElement(wrapper, byTestId("change-agent-4")).getAttribute("title")).toContain("mcp:claude-code");
        // The details of an update are listed under its summary, one per line, an empty value spelled out
        const detail = findElement(wrapper, byTestId("change-detail-2-0")).textContent ?? "";
        expect(detail).toContain("Output dataset");
        expect(detail).toContain("empty");
        expect(detail).toContain("out");
        expect(findElement(wrapper, byTestId("change-detail-2-1")).textContent).toBe("Mapping rule changed");
        expect(wrapper.container.querySelector(byTestId("change-detail-4-0"))).toBeNull();
        // Each entry offers the links the server hands out
        changes.forEach((change) =>
            change.links.forEach((link) => {
                const element = findElement(wrapper, byTestId(`change-link-${change.seq}-${link.id}`));
                expect(element.getAttribute("href")).toBe(link.path);
            }),
        );
    });

    it("should offer the revert actions in a menu, only for what can be reverted", async () => {
        const wrapper = await loadChangeList();
        // The newest change can be reverted alone; nothing is newer, so there is no revert back to before it
        expect(isDisabled(await openMenuItem(wrapper, 4, "change-revert-btn-4"))).toBe(false);
        expect(document.body.querySelector(byTestId("change-revert-back-btn-4"))).toBeNull();
        // A reverted change cannot be reverted again, but the changes after it can
        expect(isDisabled(await openMenuItem(wrapper, 2, "change-revert-btn-2"))).toBe(true);
        expect(isDisabled(findElement(document.body, byTestId("change-revert-back-btn-2")))).toBe(false);
        // A run cannot be reverted
        expect(isDisabled(await openMenuItem(wrapper, 1, "change-revert-btn-1"))).toBe(true);
    });

    it("should revert a change after confirmation and reload the list", async () => {
        const wrapper = await loadChangeList();
        fireEvent.click(await openMenuItem(wrapper, 4, "change-revert-btn-4"));
        await waitFor(() => {
            expect(findElement(document.body, byTestId("remove-item-button"))).toBeInTheDocument();
        });
        clickFoundElement(document.body, byTestId("remove-item-button"));
        await waitFor(() => {
            checkRequestMade(revertUrl(4), "POST");
        });
        mockAxios.mockResponseFor(
            { url: revertUrl(4) },
            mockedAxiosResponse({
                data: {
                    seq: 5,
                    timestamp: "2026-08-26T09:52:00.000Z",
                    type: "RemoveMapping",
                    revertible: true,
                    reverts: 4,
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

    it("should revert back to before a change, reverting a revert rather than its already reverted change", async () => {
        const wrapper = await loadChangeList();
        fireEvent.click(await openMenuItem(wrapper, 3, "change-revert-back-btn-3"));
        await waitFor(() => {
            expect(findElement(document.body, byTestId("remove-item-button"))).toBeInTheDocument();
        });
        // Back to before change 3: the mapping is undone and the revert of change 2 is undone, which redoes change 2
        expect(document.body.textContent).toContain(mappingChange.description);
        expect(document.body.textContent).toContain(revertChange.description);
        expect(document.body.textContent).not.toContain("Skipped as not revertible");
        clickFoundElement(document.body, byTestId("remove-item-button"));
        await waitFor(() => {
            checkRequestMade(revertAllUrl, "POST", { seqs: [4, 3] });
        });
        mockAxios.mockResponseFor(
            { url: revertAllUrl },
            mockedAxiosResponse({
                data: {
                    results: [
                        { seq: 4, outcome: "reverted", entry: { seq: 5, type: "RemoveMapping", revertible: true } },
                        { seq: 3, outcome: "reverted", entry: { seq: 6, type: "ReplaceTask", revertible: true } },
                    ],
                },
            }),
        );
        await waitFor(() => {
            expect(mockAxios.queue().length).toBeGreaterThan(0);
        });
        mockAxios.mockResponseFor({ url: changesUrl }, mockedAxiosResponse({ data: { reviewedUpTo: 0, changes } }));
        await waitFor(() => {
            expect(findElement(wrapper, byTestId("changes-revert-all-summary")).textContent).toContain(
                "Reverted changes: 2.",
            );
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
            checkRequestMade(reviewedUrl, "PUT", { upTo: 4 });
        });
        mockAxios.mockResponseFor({ url: reviewedUrl }, mockedAxiosResponse({ data: { reviewedUpTo: 4 } }));
        await waitFor(() => {
            expect(mockAxios.queue().length).toBeGreaterThan(0);
        });
        mockAxios.mockResponseFor(
            { url: changesUrl },
            mockedAxiosResponse({ data: { reviewedUpTo: 4, changes: reviewedChanges } }),
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
        expect(document.body.textContent).toContain(mappingChange.description);
        expect(document.body.textContent).toContain("Skipped as not revertible: 1.");
        clickFoundElement(document.body, byTestId("remove-item-button"));
        await waitFor(() => {
            checkRequestMade(revertAllUrl, "POST", { seqs: [4, 1] });
        });
        mockAxios.mockResponseFor(
            { url: revertAllUrl },
            mockedAxiosResponse({
                data: {
                    results: [
                        { seq: 4, outcome: "reverted", entry: { seq: 5, type: "RemoveMapping", revertible: true } },
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
