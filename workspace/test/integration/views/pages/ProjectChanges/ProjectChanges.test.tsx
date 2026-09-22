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
    mockAxiosResponse,
    mockedAxiosError,
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
    const conflictsUrl = (seqs: number[]) =>
        apiUrl(`/workspace/projects/${PROJECT_ID}/changes/conflicts?${seqs.map((seq) => `seq=${seq}`).join("&")}`);

    const transformLink = {
        id: "details",
        label: "Transform details page",
        path: `/workbench/projects/${PROJECT_ID}/transform/persons`,
    };
    const workflowLink = {
        id: "details",
        label: "Workflow details page",
        path: `/workbench/projects/${PROJECT_ID}/workflow/workflow`,
    };
    // Newest first: an agent mapping, the user revert of the update, the reverted update, the approved run and its proposal
    const changes: IChangeEntry[] = [
        {
            seq: 5,
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
            seq: 4,
            timestamp: "2026-08-26T09:50:40.000Z",
            user: "urn:user:alice",
            type: "ReplaceTask",
            description: "Updated transform 'persons': Output dataset 'out' → ''",
            summary: "Updated transform 'persons'",
            details: [{ label: "Output dataset", before: "out", after: "" }],
            links: [transformLink],
            revertible: true,
            reverts: 3,
        },
        {
            // A reverted entry is never flagged unreviewed
            seq: 3,
            timestamp: "2026-08-26T09:50:12.345Z",
            user: "urn:user:alice",
            origin: "mcp:claude-code",
            type: "ReplaceTask",
            description: "Updated transform 'persons': Output dataset '' → 'out', Mapping rule changed",
            summary: "Updated transform 'persons'",
            details: [{ label: "Output dataset", before: "", after: "out" }, { label: "Mapping rule changed" }],
            links: [transformLink],
            revertible: true,
            revertedBy: 4,
        },
        {
            seq: 2,
            timestamp: "2026-08-26T09:49:58.001Z",
            user: "urn:user:alice",
            origin: "mcp:claude-code",
            type: "WorkflowExecuted",
            description: "Executed workflow 'workflow'",
            summary: "Executed workflow 'workflow'",
            details: [],
            links: [
                workflowLink,
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
        {
            // The reviewed proposal the run fulfilled; final from then on
            seq: 1,
            timestamp: "2026-08-26T09:49:30.000Z",
            user: "urn:user:alice",
            origin: "mcp:claude-code",
            type: "ProposedWorkflowRun",
            description: "Proposed to run workflow 'workflow'",
            summary: "Proposed to run workflow 'workflow'",
            details: [],
            links: [workflowLink],
            revertible: false,
            fulfilledBy: 2,
        },
    ];
    const [mappingChange, revertChange] = changes;
    // The mapping change since stands in the way of restoring the whole task, which the server tells when asked
    const changedSince = `Task 'persons' in project '${PROJECT_ID}' has been changed since.`;
    const defaultConflicts = [{ seq: revertChange.seq, reason: changedSince }];

    const reviewedChanges = changes.map(({ unreviewed, ...change }) => change);

    /** Renders the list and answers the listing and, for the revertible entries of the page, the conflict check. */
    const loadChangeList = async (
        list: IChangeEntry[] = changes,
        conflicts: { seq: number; reason: string }[] = defaultConflicts,
    ): Promise<RenderResult> => {
        const wrapper = renderWrapper(<ChangeList projectId={PROJECT_ID} />);
        mockAxios.mockResponseFor(
            { url: changesUrl },
            mockedAxiosResponse({ data: { reviewedUpTo: 0, changes: list } }),
        );
        await waitFor(() => {
            expect(wrapper.container.querySelectorAll("tbody tr")).toHaveLength(list.length);
        });
        const seqs = list
            .filter((change) => change.revertible && change.revertedBy == null)
            .map((change) => change.seq);
        if (seqs.length > 0) {
            await waitFor(() => checkRequestMade(conflictsUrl(seqs), "GET"));
            mockAxios.mockResponseFor({ url: conflictsUrl(seqs) }, mockedAxiosResponse({ data: { conflicts } }));
        }
        return wrapper;
    };

    /** Answers the check the batch dialog makes when it opens, and waits until the dialog offers the revert. */
    const answerBatchCheck = async (head: number, conflicts: { seq: number; reason: string }[] = []) => {
        await waitFor(() => checkRequestMade(conflictsUrl([head]), "GET"));
        mockAxios.mockResponseFor({ url: conflictsUrl([head]) }, mockedAxiosResponse({ data: { conflicts } }));
        await waitFor(() => {
            expect(findElement(document.body, byTestId("remove-item-button"))).toBeEnabled();
        });
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

    /** The button of the open dialog with the given label. */
    const dialogButton = (label: string): Element => {
        const button = Array.from(document.body.querySelectorAll("button")).find(
            (element) => element.textContent?.trim() === label,
        );
        if (!button) {
            throw new Error(`No button '${label}' in the dialog.`);
        }
        return button;
    };

    it("should list all changes with their summaries and details", async () => {
        const wrapper = await loadChangeList();
        changes.forEach((change) => {
            expect(wrapper.container.textContent).toContain(change.summary);
        });
        // An agent change is marked by an icon carrying the origin
        expect(findElement(wrapper, byTestId("change-agent-5")).getAttribute("title")).toContain("mcp:claude-code");
        // The details of an update are listed under its summary, one per line, an empty value spelled out
        const detail = findElement(wrapper, byTestId("change-detail-3-0")).textContent ?? "";
        expect(detail).toContain("Output dataset");
        expect(detail).toContain("empty");
        expect(detail).toContain("out");
        expect(findElement(wrapper, byTestId("change-detail-3-1")).textContent).toBe("Mapping rule changed");
        expect(wrapper.container.querySelector(byTestId("change-detail-5-0"))).toBeNull();
        // The relations between entries are tagged
        expect(wrapper.container.textContent).toContain("Reverts #3");
        expect(wrapper.container.textContent).toContain("Reverted by #4");
        expect(wrapper.container.textContent).toContain("Run as #2");
        // Each entry offers the links the server hands out
        changes.forEach((change) =>
            change.links.forEach((link) => {
                const element = findElement(wrapper, byTestId(`change-link-${change.seq}-${link.id}`));
                expect(element.getAttribute("href")).toBe(link.path);
            }),
        );
    });

    it("should show only the first details of a long entry until expanded", async () => {
        const details = Array.from({ length: 8 }, (_, i) => ({ label: `Mapping rule 'rule-${i}' added` }));
        const longChange: IChangeEntry = { ...mappingChange, details };
        const wrapper = renderWrapper(<ChangeList projectId={PROJECT_ID} />);
        mockAxios.mockResponseFor(
            { url: changesUrl },
            mockedAxiosResponse({ data: { reviewedUpTo: 0, changes: [longChange] } }),
        );
        await waitFor(() => {
            expect(wrapper.container.querySelectorAll("tbody tr")).toHaveLength(1);
        });
        // Six details are shown, the rest is behind a 'more' link
        expect(findElement(wrapper, byTestId("change-detail-5-5"))).toBeInTheDocument();
        expect(wrapper.container.querySelector(byTestId("change-detail-5-6"))).toBeNull();
        clickFoundElement(wrapper, byTestId("content-blob-toggler-more-link"));
        expect(findElement(wrapper, byTestId("change-detail-5-7"))).toBeInTheDocument();
        // And hidden again with 'less'
        clickFoundElement(wrapper, byTestId("content-blob-toggler-less-link"));
        expect(wrapper.container.querySelector(byTestId("change-detail-5-6"))).toBeNull();
    });

    it("should offer the revert actions in a menu, only for what can be reverted", async () => {
        const wrapper = await loadChangeList();
        // The newest change can be reverted alone; nothing is newer, so there is no revert back to before it
        expect(isDisabled(await openMenuItem(wrapper, 5, "change-revert-btn-5"))).toBe(false);
        expect(document.body.querySelector(byTestId("change-revert-back-btn-5"))).toBeNull();
        // A change whose revert would conflict now is disabled with the reason; reverting back to before it still works,
        // as the batch reverts the newer change first, which may clear the conflict
        const conflicting = await openMenuItem(wrapper, 4, "change-revert-btn-4");
        await waitFor(() => {
            expect(isDisabled(conflicting)).toBe(true);
        });
        expect(conflicting.querySelector("[title]")?.getAttribute("title")).toBe(
            `Cannot be reverted now: ${changedSince}`,
        );
        expect(isDisabled(findElement(document.body, byTestId("change-revert-back-btn-4")))).toBe(false);
        // A reverted change cannot be reverted again, but the changes after it can
        expect(isDisabled(await openMenuItem(wrapper, 3, "change-revert-btn-3"))).toBe(true);
        expect(isDisabled(findElement(document.body, byTestId("change-revert-back-btn-3")))).toBe(false);
        // A run cannot be reverted
        expect(isDisabled(await openMenuItem(wrapper, 2, "change-revert-btn-2"))).toBe(true);
        // A fulfilled proposal cannot be discarded anymore, and the reason names the run
        const fulfilled = await openMenuItem(wrapper, 1, "change-revert-btn-1");
        expect(isDisabled(fulfilled)).toBe(true);
        expect(fulfilled.querySelector("[title]")?.getAttribute("title")).toBe("Already run as change #2");
    });

    it("should revert a change after confirmation and reload the list", async () => {
        const wrapper = await loadChangeList();
        fireEvent.click(await openMenuItem(wrapper, 5, "change-revert-btn-5"));
        await waitFor(() => {
            expect(findElement(document.body, byTestId("remove-item-button"))).toBeInTheDocument();
        });
        clickFoundElement(document.body, byTestId("remove-item-button"));
        await waitFor(() => {
            checkRequestMade(revertUrl(5), "POST");
        });
        mockAxios.mockResponseFor(
            { url: revertUrl(5) },
            mockedAxiosResponse({
                data: {
                    seq: 6,
                    timestamp: "2026-08-26T09:52:00.000Z",
                    type: "RemoveMapping",
                    revertible: true,
                    reverts: 5,
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
        fireEvent.click(await openMenuItem(wrapper, 4, "change-revert-back-btn-4"));
        await waitFor(() => {
            expect(findElement(document.body, byTestId("remove-item-button"))).toBeInTheDocument();
        });
        // Back to before change 4: the mapping is undone and the revert of change 3 is undone, which redoes change 3
        expect(document.body.textContent).toContain(mappingChange.description);
        expect(document.body.textContent).toContain(revertChange.description);
        expect(document.body.textContent).not.toContain("Skipped as not revertible");
        // The dialog offers the revert once the server has said that the newest change of the batch can be reverted,
        // and can be closed meanwhile
        expect(findElement(document.body, byTestId("remove-item-button"))).toBeDisabled();
        expect(dialogButton("Cancel")).toBeEnabled();
        await answerBatchCheck(5);
        clickFoundElement(document.body, byTestId("remove-item-button"));
        await waitFor(() => {
            checkRequestMade(revertAllUrl, "POST", { seqs: [5, 4] });
        });
        mockAxios.mockResponseFor(
            { url: revertAllUrl },
            mockedAxiosResponse({
                data: {
                    results: [
                        { seq: 5, outcome: "reverted", entry: { seq: 6, type: "RemoveMapping", revertible: true } },
                        { seq: 4, outcome: "reverted", entry: { seq: 7, type: "ReplaceTask", revertible: true } },
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
            checkRequestMade(reviewedUrl, "PUT", { upTo: 5 });
        });
        mockAxios.mockResponseFor({ url: reviewedUrl }, mockedAxiosResponse({ data: { reviewedUpTo: 5 } }));
        await waitFor(() => {
            expect(mockAxios.queue().length).toBeGreaterThan(0);
        });
        mockAxios.mockResponseFor(
            { url: changesUrl },
            mockedAxiosResponse({ data: { reviewedUpTo: 5, changes: reviewedChanges } }),
        );
        await waitFor(() => {
            expect(wrapper.container.querySelector(byTestId("changes-mark-reviewed-btn"))).not.toBeInTheDocument();
        });
    });

    it("should not keep an earlier answer when a later check fails", async () => {
        const wrapper = await loadChangeList();
        // Marking reviewed reloads the list, which asks again
        clickFoundElement(wrapper, byTestId("changes-mark-reviewed-btn"));
        await waitFor(() => {
            expect(findElement(document.body, byTestId("changes-mark-reviewed-confirm-btn"))).toBeInTheDocument();
        });
        clickFoundElement(document.body, byTestId("changes-mark-reviewed-confirm-btn"));
        await waitFor(() => {
            checkRequestMade(reviewedUrl, "PUT", { upTo: 5 });
        });
        mockAxios.mockResponseFor({ url: reviewedUrl }, mockedAxiosResponse({ data: { reviewedUpTo: 5 } }));
        await waitFor(() => {
            expect(mockAxios.queue().length).toBeGreaterThan(0);
        });
        // A fresh list, as a parsed response is; the same array would not count as a change to React
        mockAxios.mockResponseFor(
            { url: changesUrl },
            mockedAxiosResponse({ data: { reviewedUpTo: 5, changes: [...changes] } }),
        );
        // The check fails this time: the earlier reason of change 4 does not hold on, as a revert answers with the conflict itself
        await waitFor(() => checkRequestMade(conflictsUrl([5, 4]), "GET"));
        mockAxiosResponse({ url: conflictsUrl([5, 4]) }, mockedAxiosError(500));
        await waitFor(() => {
            expect(isDisabled(findElement(document.body, byTestId("change-menu-4")))).toBe(false);
        });
        expect(isDisabled(await openMenuItem(wrapper, 4, "change-revert-btn-4"))).toBe(false);
    });

    it("should not offer a batch whose newest change cannot be reverted now", async () => {
        const wrapper = await loadChangeList();
        clickFoundElement(wrapper, byTestId("changes-revert-unreviewed-btn"));
        // The dialog asks about the newest change of the batch: it conflicts, so the batch would stop at it before reverting anything
        const reason = "Rule 'name' in transform 'persons' has been changed since.";
        await waitFor(() => checkRequestMade(conflictsUrl([5]), "GET"));
        expect(findElement(document.body, byTestId("remove-item-button"))).toBeDisabled();
        mockAxios.mockResponseFor(
            { url: conflictsUrl([5]) },
            mockedAxiosResponse({ data: { conflicts: [{ seq: 5, reason }] } }),
        );
        await waitFor(() => {
            expect(findElement(document.body, byTestId("changes-revert-batch-blocked"))).toBeInTheDocument();
        });
        expect(findElement(document.body, byTestId("changes-revert-batch-blocked")).textContent).toContain(reason);
        expect(findElement(document.body, byTestId("remove-item-button"))).toBeDisabled();
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
        await answerBatchCheck(5);
        clickFoundElement(document.body, byTestId("remove-item-button"));
        await waitFor(() => {
            checkRequestMade(revertAllUrl, "POST", { seqs: [5, 2] });
        });
        mockAxios.mockResponseFor(
            { url: revertAllUrl },
            mockedAxiosResponse({
                data: {
                    results: [
                        { seq: 5, outcome: "reverted", entry: { seq: 6, type: "RemoveMapping", revertible: true } },
                        { seq: 2, outcome: "skipped", message: "Change 2 (WorkflowExecuted) cannot be reverted." },
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
