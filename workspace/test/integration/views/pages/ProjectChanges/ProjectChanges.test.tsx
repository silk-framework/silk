import React from "react";
import "@testing-library/jest-dom";
import { fireEvent, RenderResult, waitFor } from "@testing-library/react";
import { createBrowserHistory } from "history";
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
    pressKeyDown,
    renderWrapper,
    setUseParams,
    workspacePath,
} from "../../../TestHelper";
import ProjectChanges from "../../../../../src/app/views/pages/ProjectChanges";
import ChangeList from "../../../../../src/app/views/pages/ProjectChanges/ChangeList";
import { IChangeEntry } from "../../../../../src/app/views/pages/ProjectChanges/changesRequests";
import { triggerHotkeyHandler } from "../../../../../src/app/views/shared/HotKeyHandler/HotKeyHandler";
import { APP_VIEWHEADER_ID } from "../../../../../src/app/views/shared/PageHeader/PageHeader";

describe("Project changes", () => {
    afterEach(() => {
        mockAxios.reset();
        document.getElementById(APP_VIEWHEADER_ID)?.remove();
    });

    const PROJECT_ID = "cmem";
    const changesUrl = apiUrl(`/workspace/projects/${PROJECT_ID}/changes`);
    const searchUrl = apiUrl(`/workspace/projects/${PROJECT_ID}/changes/search`);
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
            kind: "added",
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
            kind: "updated",
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
            kind: "updated",
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
            kind: "run",
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
            kind: "run",
            description: "Proposed to run workflow 'workflow'",
            summary: "Proposed to run workflow 'workflow'",
            details: [],
            links: [workflowLink],
            revertible: false,
            fulfilledBy: 2,
        },
    ];
    const [mappingChange, revertChange] = changes;
    const unreviewedChanges = changes.filter((change) => change.unreviewed);
    // The mapping change since stands in the way of restoring the whole task, which the server tells when asked
    const changedSince = `Task 'persons' in project '${PROJECT_ID}' has been changed since.`;
    const defaultConflicts = [{ seq: revertChange.seq, reason: changedSince }];
    const reviewedChanges = changes.map(({ unreviewed, ...change }) => change);

    /** The answer of the search for a list, as the server builds it: the page, the state facet counting the unreviewed entries, the journal's numbers. */
    const searchAnswer = (list: IChangeEntry[], reviewedUpTo: number = 0, unreviewed?: number) => {
        const unreviewedShown = list.filter((change) => change.unreviewed).length;
        return {
            total: list.length,
            results: [...list],
            facets:
                unreviewedShown > 0
                    ? [
                          {
                              id: "changeState",
                              label: "State",
                              description: "",
                              type: "keyword",
                              values: [{ id: "unreviewed", label: "Unreviewed", count: unreviewedShown }],
                          },
                      ]
                    : [],
            sortByProperties: [{ id: "time", label: "Time" }],
            reviewedUpTo,
            latestSeq: Math.max(0, ...list.map((change) => change.seq)),
            unreviewed: unreviewed ?? unreviewedShown,
        };
    };

    /** Renders the list and answers the search and, for the revertible entries of the page, the conflict check. */
    const loadChangeList = async (
        list: IChangeEntry[] = changes,
        conflicts: { seq: number; reason: string }[] = defaultConflicts,
    ): Promise<RenderResult> => {
        const wrapper = renderWrapper(<ChangeList projectId={PROJECT_ID} />);
        mockAxios.mockResponseFor({ url: searchUrl }, mockedAxiosResponse({ data: searchAnswer(list) }));
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

    /** Answers the search that follows a revert or a review. */
    const answerReload = async (list: IChangeEntry[] = changes, reviewedUpTo: number = 0) => {
        await waitFor(() => checkRequestMade(searchUrl, "POST"));
        mockAxios.mockResponseFor({ url: searchUrl }, mockedAxiosResponse({ data: searchAnswer(list, reviewedUpTo) }));
    };

    /** Answers the search for all unreviewed entries that the batch revert of them starts with. */
    const answerUnreviewedSearch = async () => {
        await waitFor(() => checkRequestMade(searchUrl, "POST", { limit: 0 }, true));
        mockAxios.mockResponseFor({ url: searchUrl }, mockedAxiosResponse({ data: searchAnswer(unreviewedChanges) }));
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

    it("should highlight the related entries while one is hovered", async () => {
        const wrapper = await loadChangeList();
        const isRelated = (seq: number) =>
            findElement(wrapper, byTestId(`change-row-${seq}`)).className.includes("diapp-changes__row--related");
        // The revert and the change it reverted belong together; the proposal and its run too
        fireEvent.mouseEnter(findElement(wrapper, byTestId("change-row-4")));
        expect([3, 4, 5].map(isRelated)).toEqual([true, false, false]);
        fireEvent.mouseLeave(findElement(wrapper, byTestId("change-row-4")));
        expect(isRelated(3)).toBe(false);
        fireEvent.mouseEnter(findElement(wrapper, byTestId("change-row-2")));
        expect([1, 2, 3].map(isRelated)).toEqual([true, false, false]);
    });

    it("should show only the first details of a long entry until expanded", async () => {
        const details = Array.from({ length: 8 }, (_, i) => ({ label: `Mapping rule 'rule-${i}' added` }));
        const longChange: IChangeEntry = { ...mappingChange, details };
        const wrapper = renderWrapper(<ChangeList projectId={PROJECT_ID} />);
        mockAxios.mockResponseFor({ url: searchUrl }, mockedAxiosResponse({ data: searchAnswer([longChange]) }));
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
        // The list is reloaded after the revert; the rows stay until the answer arrives
        await waitFor(() => checkRequestMade(searchUrl, "POST"));
        expect(wrapper.container.querySelectorAll("tbody tr")).toHaveLength(changes.length);
        await answerReload();
        await waitFor(() => {
            expect(document.body.querySelector(byTestId("remove-item-button"))).not.toBeInTheDocument();
        });
    });

    it("should revert back to before a change, reverting a revert rather than its already reverted change", async () => {
        const wrapper = await loadChangeList();
        fireEvent.click(await openMenuItem(wrapper, 4, "change-revert-back-btn-4"));
        // The entries from the change upwards come from the journal, as the page may not hold them
        await waitFor(() => checkRequestMade(changesUrl, "GET"));
        expect(mockAxios.getReqMatching({ url: changesUrl, params: { fromSeq: 4 } })).toBeDefined();
        mockAxios.mockResponseFor(
            { url: changesUrl },
            mockedAxiosResponse({ data: { reviewedUpTo: 0, changes: changes.filter((change) => change.seq >= 4) } }),
        );
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
        await answerReload();
        await waitFor(() => {
            expect(findElement(wrapper, byTestId("changes-revert-all-summary")).textContent).toContain(
                "Reverted changes: 2.",
            );
        });
    });

    it("should offer the review actions for the unreviewed changes and mark them reviewed with the latest fetched seq", async () => {
        const wrapper = await loadChangeList();
        expect(wrapper.container.textContent).toContain("Unreviewed changes: 2");
        expect(findElement(wrapper, byTestId("changes-revert-unreviewed-btn"))).toBeInTheDocument();
        clickFoundElement(wrapper, byTestId("changes-mark-reviewed-btn"));
        await waitFor(() => {
            expect(findElement(document.body, byTestId("changes-mark-reviewed-confirm-btn"))).toBeInTheDocument();
        });
        clickFoundElement(document.body, byTestId("changes-mark-reviewed-confirm-btn"));
        await waitFor(() => {
            checkRequestMade(reviewedUrl, "PUT", { upTo: 5 });
        });
        mockAxios.mockResponseFor({ url: reviewedUrl }, mockedAxiosResponse({ data: { reviewedUpTo: 5 } }));
        await answerReload(reviewedChanges, 5);
        await waitFor(() => {
            expect(wrapper.container.querySelector(byTestId("changes-mark-reviewed-btn"))).not.toBeInTheDocument();
        });
    });

    it("should send the search to the server and not mark reviewed while it hides unreviewed changes", async () => {
        const wrapper = renderWrapper(<ChangeList projectId={PROJECT_ID} />, createBrowserHistory(), {
            workspace: { filters: { appliedFilters: { textQuery: "mapping" } } },
        });
        await waitFor(() => checkRequestMade(searchUrl, "POST", { textQuery: "mapping" }, true));
        // One of the two unreviewed changes matches: marking all as reviewed would approve the other one unseen
        mockAxios.mockResponseFor(
            { url: searchUrl },
            mockedAxiosResponse({ data: searchAnswer([mappingChange], 0, 2) }),
        );
        await waitFor(() => {
            expect(wrapper.container.querySelectorAll("tbody tr")).toHaveLength(1);
        });
        expect(wrapper.container.textContent).toContain("Unreviewed changes: 2");
        expect(findElement(wrapper, byTestId("changes-mark-reviewed-btn"))).toBeDisabled();
        expect(findElement(wrapper, byTestId("changes-revert-unreviewed-btn"))).toBeEnabled();
    });

    it("should not mark reviewed while the state facet hides the unreviewed changes, whatever the facet counts", async () => {
        const wrapper = renderWrapper(<ChangeList projectId={PROJECT_ID} />, createBrowserHistory(), {
            workspace: {
                filters: { appliedFacets: [{ facetId: "changeState", type: "keyword", keywordIds: ["reverted"] }] },
            },
        });
        await waitFor(() => checkRequestMade(searchUrl, "POST"));
        // The state facet counts its values as if it were not applied, so the count still says 2 while none is shown
        const reverted = changes.filter((change) => change.revertedBy != null);
        mockAxios.mockResponseFor(
            { url: searchUrl },
            mockedAxiosResponse({
                data: { ...searchAnswer(reverted, 0, 2), facets: searchAnswer(unreviewedChanges).facets },
            }),
        );
        await waitFor(() => {
            expect(wrapper.container.querySelectorAll("tbody tr")).toHaveLength(reverted.length);
        });
        expect(findElement(wrapper, byTestId("changes-mark-reviewed-btn"))).toBeDisabled();
    });

    it("should report a failed search instead of an empty journal", async () => {
        const wrapper = renderWrapper(<ChangeList projectId={PROJECT_ID} />);
        await waitFor(() => checkRequestMade(searchUrl, "POST"));
        mockAxiosResponse(
            searchUrl,
            mockedAxiosError(500, { title: "Server error", detail: "The journal is unreadable." }),
        );
        await waitFor(() => {
            expect(wrapper.container.textContent).toContain("The journal is unreadable.");
        });
        expect(wrapper.container.textContent).not.toContain("No changes have been recorded");
    });

    it("should not offer an empty batch when nothing newer can be reverted", async () => {
        // The run and the proposal it fulfilled: nothing after the proposal can be reverted
        const runAndProposal = changes.slice(3);
        const wrapper = await loadChangeList(runAndProposal, []);
        fireEvent.click(await openMenuItem(wrapper, 1, "change-revert-back-btn-1"));
        await waitFor(() => checkRequestMade(changesUrl, "GET"));
        mockAxios.mockResponseFor(
            { url: changesUrl },
            mockedAxiosResponse({ data: { reviewedUpTo: 0, changes: runAndProposal } }),
        );
        await waitFor(() => {
            expect(findElement(document.body, byTestId("changes-revert-batch-nothing"))).toBeInTheDocument();
        });
        expect(findElement(document.body, byTestId("remove-item-button"))).toBeDisabled();
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
        await answerReload(changes, 5);
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
        await answerUnreviewedSearch();
        // The dialog asks about the newest change of the batch: it conflicts, so the batch would stop at it before reverting anything
        const reason = "Rule 'name' in transform 'persons' has been changed since.";
        await waitFor(() => checkRequestMade(conflictsUrl([5]), "GET"));
        expect(findElement(document.body, byTestId("remove-item-button"))).toBeDisabled();
        // Enter does not do what the button refuses, neither while checking nor once blocked
        triggerHotkeyHandler("enter");
        expect(mockAxios.getReqMatching({ url: revertAllUrl })).toBeUndefined();
        mockAxios.mockResponseFor(
            { url: conflictsUrl([5]) },
            mockedAxiosResponse({ data: { conflicts: [{ seq: 5, reason }] } }),
        );
        await waitFor(() => {
            expect(findElement(document.body, byTestId("changes-revert-batch-blocked"))).toBeInTheDocument();
        });
        expect(findElement(document.body, byTestId("changes-revert-batch-blocked")).textContent).toContain(reason);
        expect(findElement(document.body, byTestId("remove-item-button"))).toBeDisabled();
        triggerHotkeyHandler("enter");
        expect(mockAxios.getReqMatching({ url: revertAllUrl })).toBeUndefined();
    });

    it("should revert the unreviewed changes and report the outcome", async () => {
        const wrapper = await loadChangeList();
        clickFoundElement(wrapper, byTestId("changes-revert-unreviewed-btn"));
        await answerUnreviewedSearch();
        await waitFor(() => {
            expect(findElement(document.body, byTestId("remove-item-button"))).toBeInTheDocument();
        });
        // The dialog lists what will be attempted and notes the non-revertible entry that will be skipped
        expect(document.body.textContent).toContain(mappingChange.description);
        expect(document.body.textContent).toContain("Skipped as not revertible: 1.");
        await answerBatchCheck(5);
        // Enter confirms once the revert is offered
        triggerHotkeyHandler("enter");
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
        await answerReload();
        await waitFor(() => {
            const summary = findElement(wrapper, byTestId("changes-revert-all-summary"));
            expect(summary.textContent).toContain("Reverted changes: 1.");
            expect(summary.textContent).toContain("Skipped: 1.");
        });
    });

    it("should explain the change journal in a help dialog", async () => {
        setUseParams(PROJECT_ID, "");
        const wrapper = renderWrapper(<ProjectChanges />);
        clickFoundElement(wrapper, byTestId("changes-help-btn"));
        await waitFor(() => {
            const dialog = findElement(document.body, byTestId("changes-help-modal"));
            expect(dialog.textContent).toContain("The change journal");
            expect(dialog.textContent).toContain("Mark all as reviewed");
        });
    });

    it("should offer time as the only sort property, active newest first", async () => {
        setUseParams(PROJECT_ID, "");
        const wrapper = renderWrapper(<ProjectChanges />);
        await waitFor(() => checkRequestMade(searchUrl, "POST"));
        mockAxios.mockResponseFor({ url: searchUrl }, mockedAxiosResponse({ data: searchAnswer(changes) }));
        await waitFor(() => {
            expect(wrapper.container.querySelectorAll("tbody tr")).toHaveLength(changes.length);
        });
        clickFoundElement(wrapper, `${byTestId("sortButton")} button`);
        await waitFor(() => {
            expect(findElement(document.body, byTestId("sort-option-time-DESC"))).toBeInTheDocument();
        });
        expect(document.body.querySelector('[data-test-id^="sort-option-default"]')).toBeNull();
    });

    it("should take nothing another page left in the search store: its items, facets or error", async () => {
        setUseParams(PROJECT_ID, "");
        // As after the project page: its tasks are the store's results until this page has searched
        const wrapper = renderWrapper(<ProjectChanges />, createBrowserHistory(), {
            workspace: {
                preview: {
                    searchResults: [{ id: "persons", label: "Persons", type: "dataset" }],
                    error: { detail: "Stale error" },
                },
                filters: {
                    facets: [
                        {
                            id: "staleFacet",
                            label: "Stale facet",
                            description: "",
                            type: "keyword",
                            values: [{ id: "x", label: "X", count: 1 }],
                        },
                    ],
                },
            },
        });
        await waitFor(() => checkRequestMade(searchUrl, "POST"));
        expect(wrapper.container.querySelectorAll("tbody tr")).toHaveLength(0);
        expect(wrapper.container.textContent).not.toContain("Stale facet");
        expect(wrapper.container.textContent).not.toContain("Stale error");
        mockAxios.mockResponseFor({ url: searchUrl }, mockedAxiosResponse({ data: searchAnswer(changes) }));
        await waitFor(() => {
            expect(wrapper.container.querySelectorAll("tbody tr")).toHaveLength(changes.length);
        });
    });

    it("should set the search up from the page's URL", async () => {
        setUseParams(PROJECT_ID, "");
        // The review link of the MCP server opens the page filtered to the changes awaiting review
        const history = createBrowserHistory();
        history.location.pathname = workspacePath(`/projects/${PROJECT_ID}/changes`);
        history.location.search = "?f_ids=changeState&types=keyword&f_keys=unreviewed";
        renderWrapper(<ProjectChanges />, history);
        await waitFor(() => checkRequestMade(searchUrl, "POST"));
        expect(mockAxios.getReqMatching({ url: searchUrl })?.data).toMatchObject({
            facets: [{ facetId: "changeState", type: "keyword", keywordIds: ["unreviewed"] }],
        });
    });

    it("should reload the list on Enter in the search field, keeping the query and the facets", async () => {
        setUseParams(PROJECT_ID, "");
        const history = createBrowserHistory();
        history.location.pathname = workspacePath(`/projects/${PROJECT_ID}/changes`);
        history.location.search = "?textQuery=persons&f_ids=changeState&types=keyword&f_keys=unreviewed";
        const wrapper = renderWrapper(<ProjectChanges />, history);
        await waitFor(() => checkRequestMade(searchUrl, "POST", { textQuery: "persons" }, true));
        mockAxios.mockResponseFor({ url: searchUrl }, mockedAxiosResponse({ data: searchAnswer(changes) }));
        await waitFor(() => {
            expect(wrapper.container.querySelectorAll("tbody tr")).toHaveLength(changes.length);
        });
        // The search bar also fires for the query it was handed, after its debounce: no second search for it
        await new Promise((resolve) => setTimeout(resolve, 600));
        expect(mockAxios.queue().filter((item) => item.url === searchUrl)).toHaveLength(0);
        // Enter reloads; the search bar's default would search the unchanged query and clear the facets
        pressKeyDown(findElement(wrapper, byTestId("search-bar")) as HTMLElement);
        await waitFor(() => checkRequestMade(searchUrl, "POST"));
        expect(mockAxios.getReqMatching({ url: searchUrl })?.data).toMatchObject({
            textQuery: "persons",
            facets: [{ facetId: "changeState", type: "keyword", keywordIds: ["unreviewed"] }],
        });
    });

    it("should link the breadcrumbs to the project list and to the project's default page", async () => {
        // The page header renders into the application's header slot
        const headerRoot = document.body.appendChild(document.createElement("div"));
        headerRoot.id = APP_VIEWHEADER_ID;
        setUseParams(PROJECT_ID, "");
        renderWrapper(<ProjectChanges />);
        const metaDataUrl = apiUrl(`/workspace/projects/${PROJECT_ID}/metaData`);
        await waitFor(() => checkRequestMade(metaDataUrl, "GET"));
        mockAxiosResponse(metaDataUrl, mockedAxiosResponse({ data: { label: "CMEM" } }));
        await waitFor(() => expect(headerRoot.textContent).toContain("Changes: CMEM"));
        // Without a layout, the breadcrumbs before the current one are collapsed into a menu
        fireEvent.click(findElement(headerRoot, '[aria-label="collapsed breadcrumbs"]'));
        await waitFor(() => {
            const links = Array.from(document.body.querySelectorAll('a[role="menuitem"][href]')).map((link) =>
                link.getAttribute("href"),
            );
            expect(links).toEqual(
                expect.arrayContaining(["/workbench?itemType=project", `/workbench/projects/${PROJECT_ID}`]),
            );
        });
    });

    it("should keep nothing of the previous project when the project changes", async () => {
        const metaDataUrl = (projectId: string) => apiUrl(`/workspace/projects/${projectId}/metaData`);
        const notFoundPage = () => wrapper.container.querySelector(byTestId("not-found-page"));
        const rows = () => wrapper.container.querySelectorAll("tbody tr");
        // As the router does it: the page stays and only its project id changes
        const switchProject = (projectId: string) => {
            setUseParams(projectId, "");
            wrapper.rerender(<ProjectChanges />);
        };

        setUseParams("missingProject", "");
        const wrapper = renderWrapper(<ProjectChanges />);
        await waitFor(() => checkRequestMade(metaDataUrl("missingProject"), "GET"));
        mockAxiosResponse(metaDataUrl("missingProject"), mockedAxiosError(404));
        await waitFor(() => {
            expect(notFoundPage()).toBeInTheDocument();
        });

        // An existing project is not reported missing
        switchProject(PROJECT_ID);
        await waitFor(() => checkRequestMade(searchUrl, "POST"));
        expect(notFoundPage()).not.toBeInTheDocument();
        mockAxios.mockResponseFor({ url: searchUrl }, mockedAxiosResponse({ data: searchAnswer(changes) }));
        await waitFor(() => {
            expect(rows()).toHaveLength(changes.length);
        });

        // Another project does not show the changes of the previous one while it loads
        switchProject("otherProject");
        const otherSearchUrl = apiUrl("/workspace/projects/otherProject/changes/search");
        await waitFor(() => checkRequestMade(otherSearchUrl, "POST"));
        expect(rows()).toHaveLength(0);
        // An answer for the previous project that arrives late is not taken for this one
        mockAxiosResponse(metaDataUrl(PROJECT_ID), mockedAxiosError(404));
        mockAxios.mockResponseFor(
            { url: otherSearchUrl },
            mockedAxiosResponse({ data: searchAnswer([mappingChange]) }),
        );
        await waitFor(() => {
            expect(rows()).toHaveLength(1);
        });
        expect(notFoundPage()).not.toBeInTheDocument();
    });
});
