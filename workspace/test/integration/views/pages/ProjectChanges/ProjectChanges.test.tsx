import React from "react";
import "@testing-library/jest-dom";
import { act, fireEvent, RenderResult, waitFor, within } from "@testing-library/react";
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
import { useDispatch } from "react-redux";
import { workspaceOp } from "@ducks/workspace";
import { AppDispatch } from "../../../../../src/app/store/configureStore";
import {
    ChangeConversationsContext,
    ChangeConversationIdentity,
    OpenChangeConversationContext,
} from "../../../../../src/app/views/pages/ProjectChanges/ChangeConversationsContext";
import {
    ChangeConversationLabel,
    CONVERSATION_FACET,
} from "../../../../../src/app/views/pages/ProjectChanges/ChangeSource";
import Filterbar from "../../../../../src/app/views/pages/Workspace/Filterbar";
import { APP_VIEWHEADER_ID } from "../../../../../src/app/views/shared/PageHeader/PageHeader";
import { useSelector } from "react-redux";
import { workspaceSel } from "@ducks/workspace";

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
            conversationId: "conversation-a",
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
            conversationId: "conversation-b",
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
            conversationId: "unavailable",
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
        const conversationFacet = {
            id: "changeConversation",
            label: "Conversation",
            type: "keyword",
            values: Array.from(
                new Set(list.flatMap((change) => (change.conversationId ? [change.conversationId] : []))),
            ).map((id) => ({ id, label: id, count: list.filter((change) => change.conversationId === id).length })),
        };
        return {
            total: list.length,
            results: [...list],
            facets: [
                conversationFacet,
                ...(unreviewedShown > 0
                    ? [
                          {
                              id: "changeState",
                              label: "State",
                              description: "",
                              type: "keyword",
                              values: [{ id: "unreviewed", label: "Unreviewed", count: unreviewedShown }],
                          },
                      ]
                    : []),
            ],
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
        identities?: ReadonlyMap<string, ChangeConversationIdentity>,
        history = createBrowserHistory(),
        onOpenConversation?: (id: string) => void,
    ): Promise<RenderResult> => {
        const listElement = <ChangeList projectId={PROJECT_ID} />;
        const wrapper = renderWrapper(
            identities ? (
                <OpenChangeConversationContext.Provider value={onOpenConversation}>
                    <ChangeConversationsContext.Provider value={identities}>
                        <Filterbar
                            facetsOnly
                            renderValueLabel={(facetId, value) =>
                                facetId === CONVERSATION_FACET ? (
                                    <ChangeConversationLabel conversationId={value.id} />
                                ) : undefined
                            }
                        />
                        {listElement}
                    </ChangeConversationsContext.Provider>
                </OpenChangeConversationContext.Provider>
            ) : (
                listElement
            ),
            history,
        );
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

    const answerReviewPreview = async (entries: IChangeEntry[] = unreviewedChanges) => {
        await waitFor(() =>
            checkRequestMade(
                searchUrl,
                "POST",
                {
                    limit: 0,
                    facets: [{ facetId: "changeState", type: "keyword", keywordIds: ["unreviewed"] }],
                },
                true,
            ),
        );
        mockAxios.mockResponseFor({ url: searchUrl }, mockedAxiosResponse({ data: searchAnswer(entries) }));
        await waitFor(() =>
            expect(findElement(document.body, byTestId("changes-mark-reviewed-confirm-btn"))).toBeEnabled(),
        );
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
        expect(findElement(wrapper, byTestId("change-agent-1"))).toHaveAccessibleName("AI-origin change");
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

    it("should show either the conversation or AI indicator in Source beside the user", async () => {
        const wrapper = await loadChangeList();
        expect(wrapper.getAllByRole("columnheader").map((header) => header.textContent)).toEqual([
            "#",
            "Date",
            "User",
            "Source",
            "Change",
            "",
        ]);
        const agentCells = findElement(wrapper, byTestId("change-row-5")).querySelectorAll("td");
        const conversationBadge = findElement(wrapper, byTestId("change-conversation-5"));
        expect(agentCells[2]).toHaveTextContent("alice");
        expect(agentCells[2]).not.toContainElement(conversationBadge);
        expect(agentCells[3]).toContainElement(conversationBadge);
        expect(agentCells[3].querySelectorAll("svg")).toHaveLength(1);
        expect(wrapper.container.querySelector(byTestId("change-agent-5"))).toBeNull();
        const agentIndicator = findElement(wrapper, byTestId("change-agent-1"));
        const standaloneAgentCells = findElement(wrapper, byTestId("change-row-1")).querySelectorAll("td");
        expect(standaloneAgentCells[3]).toContainElement(agentIndicator);
        expect(standaloneAgentCells[3].querySelectorAll("svg")).toHaveLength(1);
        expect(agentIndicator).toHaveAccessibleName("AI-origin change");
        expect(agentIndicator).not.toHaveAttribute("title");
        const userCells = findElement(wrapper, byTestId("change-row-4")).querySelectorAll("td");
        expect(userCells[2]).toHaveTextContent("alice");
        expect(userCells[3]).toBeEmptyDOMElement();
    });

    it("should use the same numbered badge for a conversation and send its ID when filtering", async () => {
        const identities = new Map([
            ["conversation-a", { number: 2, title: "Duplicate title" }],
            ["conversation-b", { number: 7, title: "Duplicate title" }],
        ]);
        const wrapper = await loadChangeList(changes, defaultConflicts, identities);
        expect(findElement(wrapper, byTestId("change-conversation-5"))).toHaveTextContent("C2");
        expect(wrapper.getByRole("checkbox", { name: /C2/ })).toHaveAttribute("value", "conversation-a");
        expect(findElement(wrapper, byTestId("change-conversation-3"))).toHaveTextContent("C7");
        const conversationBadge = findElement(wrapper, byTestId("change-conversation-5"));
        expect(conversationBadge).toHaveAccessibleName("Filter by conversation C2");
        expect(conversationBadge).toHaveAttribute("aria-describedby");
        expect(wrapper.container.querySelector(byTestId("change-agent-5"))).toBeNull();
        expect(findElement(wrapper, byTestId("change-conversation-2"))).toHaveTextContent("?");
        expect(findElement(wrapper, byTestId("change-conversation-2"))).toHaveAccessibleName(
            "Filter by conversation ?",
        );
        expect(wrapper.container.querySelector(byTestId("change-conversation-1"))).toBeNull();
        clickFoundElement(wrapper, byTestId("change-conversation-5"));
        await waitFor(() =>
            checkRequestMade(
                searchUrl,
                "POST",
                {
                    facets: [{ facetId: "changeConversation", type: "keyword", keywordIds: ["conversation-a"] }],
                },
                true,
            ),
        );
        mockAxios.mockResponseFor(
            { url: searchUrl },
            mockedAxiosResponse({ data: searchAnswer([mappingChange], 0, unreviewedChanges.length) }),
        );
        await waitFor(() => expect(wrapper.container.querySelectorAll("tbody tr")).toHaveLength(1));
        expect(findElement(wrapper, byTestId("change-conversation-5"))).toHaveTextContent("C2");
        expect(findElement(wrapper, byTestId("change-conversation-5"))).toHaveAttribute("aria-pressed", "true");
        expect(findElement(wrapper, byTestId("changes-mark-reviewed-btn"))).toBeDisabled();
        fireEvent.click(wrapper.getByRole("checkbox", { name: /C2/ }));
        await waitFor(() => checkRequestMade(searchUrl, "POST", { facets: [] }, true));
    });

    it("should link to an available companion conversation while preserving journal filters and the hash", async () => {
        const conversationId = "chat 1&two/?#";
        const history = createBrowserHistory();
        history.location.search =
            "?textQuery=persons&f_ids=changeState&types=keyword&f_keys=unreviewed&conversationId=previous";
        history.location.hash = "#change-5";
        const entry = { ...mappingChange, conversationId };
        const wrapper = await loadChangeList([entry], [], new Map([[conversationId, { number: 1 }]]), history);
        const action = findElement(wrapper, byTestId("change-open-conversation-5"));
        expect(action.tagName).toBe("A");
        expect(action).toHaveAccessibleName("Open companion conversation");
        const target = new URL(
            action.getAttribute("href")!,
            "http://localhost/dataintegration/workbench/projects/cmem/changes",
        );
        expect(target.pathname).toBe("/dataintegration/workbench/projects/cmem/changes");
        expect(target.searchParams.getAll("conversationId")).toEqual([conversationId]);
        expect(target.searchParams.get("textQuery")).toBe("persons");
        expect(target.searchParams.get("f_ids")).toBe("changeState");
        expect(target.searchParams.get("types")).toBe("keyword");
        expect(target.searchParams.get("f_keys")).toBe("unreviewed");
        expect(target.hash).toBe("#change-5");
        expect(action).not.toHaveAttribute("target");
    });

    it("should ask the live host to open the real conversation ID and retain the row", async () => {
        const open = jest.fn();
        const wrapper = await loadChangeList(
            changes,
            defaultConflicts,
            new Map([["conversation-a", { number: 7 }]]),
            createBrowserHistory(),
            open,
        );
        const row = findElement(wrapper, byTestId("change-row-5"));
        fireEvent.click(findElement(wrapper, byTestId("change-open-conversation-5")));
        expect(open).toHaveBeenCalledWith("conversation-a");
        expect(findElement(wrapper, byTestId("change-row-5"))).toBe(row);
    });

    it("should only offer opening conversations available to the current user", async () => {
        const wrapper = await loadChangeList(
            changes,
            defaultConflicts,
            new Map([["conversation-a", { number: 1, title: "Own conversation" }]]),
        );
        expect(findElement(wrapper, byTestId("change-open-conversation-5"))).toBeInTheDocument();
        [1, 2, 3, 4].forEach((seq) => {
            expect(wrapper.container.querySelector(byTestId(`change-open-conversation-${seq}`))).toBeNull();
        });
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
        expect(findElement(document.body, byTestId("changes-revert-batch-modal"))).toHaveTextContent(
            "Changes to revert: 2.",
        );
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
        expect(findElement(document.body, byTestId("changes-mark-reviewed-confirm-btn"))).toBeDisabled();
        await answerReviewPreview();
        const dialog = findElement(document.body, byTestId("changes-mark-reviewed-modal"));
        expect(dialog).toHaveTextContent("Changes to mark as reviewed: 2.");
        expect(Array.from(dialog.querySelectorAll("li"), (item) => item.textContent)).toEqual(
            unreviewedChanges.map((entry) => entry.description),
        );
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

    it("should mark reviewed up to an unreviewed change from its menu", async () => {
        const wrapper = await loadChangeList();
        // The user's own change needs no review, so its menu offers none
        await openMenuItem(wrapper, 4, "change-revert-btn-4");
        expect(document.body.querySelector(byTestId("change-mark-reviewed-btn-4"))).not.toBeInTheDocument();
        fireEvent.click(await openMenuItem(wrapper, 2, "change-mark-reviewed-btn-2"));
        await waitFor(() => {
            expect(document.body.textContent).toContain("Mark change #2 and all older changes as reviewed?");
        });
        await answerReviewPreview();
        const dialog = findElement(document.body, byTestId("changes-mark-reviewed-modal"));
        expect(dialog).toHaveTextContent("Changes to mark as reviewed: 1.");
        expect(Array.from(dialog.querySelectorAll("li"), (item) => item.textContent)).toEqual([changes[3].description]);
        clickFoundElement(document.body, byTestId("changes-mark-reviewed-confirm-btn"));
        await waitFor(() => {
            checkRequestMade(reviewedUrl, "PUT", { upTo: 2 });
        });
    });

    it("previews unreviewed changes across all pages and excludes entries newer than the fetched review boundary", async () => {
        const wrapper = renderWrapper(<ChangeList projectId={PROJECT_ID} />, createBrowserHistory(), {
            workspace: { filters: { pagination: { current: 2, limit: 25, offset: 25 } } },
        });
        await waitFor(() => checkRequestMade(searchUrl, "POST", { offset: 25 }, true));
        mockAxios.mockResponseFor(
            { url: searchUrl },
            mockedAxiosResponse({
                data: {
                    ...searchAnswer(unreviewedChanges),
                    results: [changes[3]],
                    total: 30,
                },
            }),
        );
        await waitFor(() => expect(wrapper.container.querySelectorAll("tbody tr")).toHaveLength(1));
        clickFoundElement(wrapper, byTestId("changes-mark-reviewed-btn"));
        const newerChange = { ...mappingChange, seq: 6, description: "A change added after the page loaded" };
        await answerReviewPreview([newerChange, ...unreviewedChanges]);
        const dialog = findElement(document.body, byTestId("changes-mark-reviewed-modal"));
        expect(dialog).toHaveTextContent("Changes to mark as reviewed: 2.");
        expect(Array.from(dialog.querySelectorAll("li"), (item) => item.textContent)).toEqual(
            unreviewedChanges.map((entry) => entry.description),
        );
        expect(dialog).not.toHaveTextContent(newerChange.description);
        clickFoundElement(document.body, byTestId("changes-mark-reviewed-confirm-btn"));
        await waitFor(() => checkRequestMade(reviewedUrl, "PUT", { upTo: 5 }));
    });

    it("keeps review confirmation disabled after a failed preview and supports retry", async () => {
        const wrapper = await loadChangeList();
        clickFoundElement(wrapper, byTestId("changes-mark-reviewed-btn"));
        await waitFor(() => checkRequestMade(searchUrl, "POST", { limit: 0 }, true));
        mockAxiosResponse(
            { url: searchUrl },
            mockedAxiosError(500, { title: "Server error", detail: "Preview failed." }),
        );
        const dialog = findElement(document.body, byTestId("changes-mark-reviewed-modal"));
        const retry = await within(dialog).findByRole("button", { name: "Retry" });
        expect(findElement(dialog, byTestId("changes-mark-reviewed-confirm-btn"))).toBeDisabled();
        expect(mockAxios.getReqMatching({ url: reviewedUrl })).toBeUndefined();
        fireEvent.click(retry);
        await answerReviewPreview();
        expect(dialog).toHaveTextContent("Changes to mark as reviewed: 2.");
    });

    it("does not offer review confirmation when no unreviewed changes remain", async () => {
        const wrapper = await loadChangeList();
        clickFoundElement(wrapper, byTestId("changes-mark-reviewed-btn"));
        await waitFor(() => checkRequestMade(searchUrl, "POST", { limit: 0 }, true));
        mockAxios.mockResponseFor({ url: searchUrl }, mockedAxiosResponse({ data: searchAnswer([]) }));
        const dialog = findElement(document.body, byTestId("changes-mark-reviewed-modal"));
        await waitFor(() => expect(dialog).toHaveTextContent("Changes to mark as reviewed: 0."));
        expect(findElement(dialog, byTestId("changes-mark-reviewed-confirm-btn"))).toBeDisabled();
        expect(dialog).toHaveTextContent("No unreviewed changes remain in this selection.");
        expect(mockAxios.getReqMatching({ url: reviewedUrl })).toBeUndefined();
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
        expect(isDisabled(await openMenuItem(wrapper, 5, "change-mark-reviewed-btn-5"))).toBe(true);
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
        expect(findElement(document.body, byTestId("changes-revert-batch-modal"))).toHaveTextContent(
            "Changes to revert: 0.",
        );
        expect(findElement(document.body, byTestId("remove-item-button"))).toBeDisabled();
    });

    it("should not keep an earlier answer when a later check fails", async () => {
        const wrapper = await loadChangeList();
        // Marking reviewed reloads the list, which asks again
        clickFoundElement(wrapper, byTestId("changes-mark-reviewed-btn"));
        await waitFor(() => {
            expect(findElement(document.body, byTestId("changes-mark-reviewed-confirm-btn"))).toBeInTheDocument();
        });
        await answerReviewPreview();
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
        expect(findElement(document.body, byTestId("changes-revert-batch-modal"))).toHaveTextContent(
            "Changes to revert: 1.",
        );
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

    it("shows removable active filters above empty results and preserves the remaining search and review context", async () => {
        setUseParams(PROJECT_ID, "");
        const history = createBrowserHistory();
        history.location.pathname = workspacePath(`/projects/${PROJECT_ID}/changes`);
        history.location.search = `?${new URLSearchParams({
            textQuery: "movies",
            page: "2",
            highlightConversation: "conversation-a",
            f_ids: "changeState,changeConversation",
            types: "keyword,keyword",
            f_keys: "unreviewed,conversation-a",
        }).toString()}`;
        const wrapper = renderWrapper(<ProjectChanges />, history);
        await waitFor(() => checkRequestMade(searchUrl, "POST", { offset: 25 }, true));
        mockAxios.mockResponseFor({ url: searchUrl }, mockedAxiosResponse({ data: searchAnswer([]) }));

        const emptyResults = await wrapper.findByText("No changes match the search.");
        const state = findElement(wrapper, byTestId("applied-facet-changeState"));
        const conversation = findElement(wrapper, byTestId("applied-facet-changeConversation"));
        expect(state).toHaveTextContent("Unreviewed");
        expect(conversation).toHaveTextContent("?");
        expect(conversation).not.toHaveTextContent("conversation-a");
        expect(conversation.querySelector(".diapp-changes__conversation--highlighted")).not.toBeNull();
        expect(conversation.compareDocumentPosition(emptyResults) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();

        fireEvent.click(within(conversation).getByRole("button", { name: "Remove tag" }));
        await waitFor(() =>
            checkRequestMade(
                searchUrl,
                "POST",
                {
                    textQuery: "movies",
                    offset: 0,
                    facets: [{ facetId: "changeState", type: "keyword", keywordIds: ["unreviewed"] }],
                },
                true,
            ),
        );
        const query = new URLSearchParams(history.location.search);
        expect(query.get("highlightConversation")).toBe("conversation-a");
        expect(query.get("textQuery")).toBe("movies");
        expect(query.get("page")).toBe("1");
        expect(query.get("f_ids")).toBe("changeState");
        expect(wrapper.container.querySelector(byTestId("applied-facet-changeConversation"))).toBeNull();
        mockAxios.mockResponseFor({ url: searchUrl }, mockedAxiosResponse({ data: searchAnswer([]) }));
        fireEvent.click(within(state).getByRole("button", { name: "Remove tag" }));
        await waitFor(() => checkRequestMade(searchUrl, "POST", { facets: [], textQuery: "movies" }, true));
        expect(wrapper.container.querySelector(byTestId("applied-facet-changeState"))).toBeNull();
        mockAxios.mockResponseFor({ url: searchUrl }, mockedAxiosResponse({ data: searchAnswer([]) }));
    });

    it("highlights and prioritizes the linked conversation, preserves its URL context, and applies review links without remounting", async () => {
        setUseParams(PROJECT_ID, "");
        const history = createBrowserHistory();
        const path = workspacePath(`/projects/${PROJECT_ID}/changes`);
        const highlight = "conversation-8";
        const stateFacet = { facetId: "changeState", type: "keyword", keywordIds: ["unreviewed"] };
        const projectQuery = new URLSearchParams({
            highlightConversation: highlight,
            f_ids: "changeState",
            types: "keyword",
            f_keys: "unreviewed",
        });
        const conversationQuery = new URLSearchParams(projectQuery);
        conversationQuery.set("f_ids", "changeState,changeConversation");
        conversationQuery.set("types", "keyword,keyword");
        conversationQuery.set("f_keys", `unreviewed,${highlight}`);
        history.location.pathname = path;
        history.location.search = `?${projectQuery.toString()}`;
        const entries = Array.from({ length: 9 }, (_, i) => ({
            ...mappingChange,
            seq: i + 1,
            conversationId: `conversation-${i}`,
            revertible: false,
        }));
        const wrapper = renderWrapper(<ProjectChanges />, history);
        await waitFor(() => checkRequestMade(searchUrl, "POST", { facets: [stateFacet] }, true));
        mockAxios.mockResponseFor({ url: searchUrl }, mockedAxiosResponse({ data: searchAnswer(entries) }));
        await waitFor(() => expect(wrapper.container.querySelectorAll("tbody tr")).toHaveLength(9));
        const facetInputs = () =>
            Array.from(
                findElement(wrapper, byTestId("search-facets")).querySelectorAll<HTMLInputElement>(
                    'input[type="checkbox"]',
                ),
            );
        expect(facetInputs()[0].value).toBe(highlight);
        expect(facetInputs()[0]).not.toBeChecked();
        expect(facetInputs().find((input) => input.value === "unreviewed")).toBeChecked();
        const badge = findElement(wrapper, byTestId("change-conversation-9"));
        expect(badge).toHaveClass("diapp-changes__conversation--highlighted");
        expect(badge).toHaveAttribute("aria-pressed", "false");
        expect(findElement(wrapper, byTestId("change-conversation-1"))).not.toHaveClass(
            "diapp-changes__conversation--highlighted",
        );
        expect(new URLSearchParams(history.location.search).get("highlightConversation")).toBe(highlight);

        act(() => history.push(`${path}?${conversationQuery.toString()}`));
        await waitFor(() =>
            checkRequestMade(
                searchUrl,
                "POST",
                { facets: [stateFacet, { facetId: "changeConversation", type: "keyword", keywordIds: [highlight] }] },
                true,
            ),
        );
        mockAxios.mockResponseFor({ url: searchUrl }, mockedAxiosResponse({ data: searchAnswer([entries[8]], 0, 9) }));
        await waitFor(() => expect(wrapper.container.querySelectorAll("tbody tr")).toHaveLength(1));
        expect(findElement(wrapper, byTestId("change-conversation-9"))).toHaveAttribute("aria-pressed", "true");
        expect(findElement(wrapper, byTestId("changes-mark-reviewed-btn"))).toBeDisabled();

        act(() => history.push(`${path}?${projectQuery.toString()}`));
        await waitFor(() => checkRequestMade(searchUrl, "POST", { facets: [stateFacet] }, true));
        mockAxios.mockResponseFor({ url: searchUrl }, mockedAxiosResponse({ data: searchAnswer(entries) }));
        await waitFor(() => expect(wrapper.container.querySelectorAll("tbody tr")).toHaveLength(9));
        expect(findElement(wrapper, byTestId("changes-mark-reviewed-btn"))).not.toBeDisabled();
        clickFoundElement(wrapper, byTestId("change-conversation-1"));
        await waitFor(() =>
            checkRequestMade(
                searchUrl,
                "POST",
                {
                    facets: [
                        stateFacet,
                        { facetId: "changeConversation", type: "keyword", keywordIds: ["conversation-0"] },
                    ],
                },
                true,
            ),
        );
        expect(new URLSearchParams(history.location.search).get("highlightConversation")).toBe(highlight);
        mockAxios.mockResponseFor({ url: searchUrl }, mockedAxiosResponse({ data: searchAnswer([entries[0]], 0, 9) }));
        await waitFor(() => expect(wrapper.container.querySelectorAll("tbody tr")).toHaveLength(1));
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

    it("preserves review context for journal pagination and stops preserving it after leaving the journal", async () => {
        setUseParams(PROJECT_ID, "");
        const history = createBrowserHistory();
        history.location.pathname = workspacePath(`/projects/${PROJECT_ID}/changes`);
        history.location.search = "?highlightConversation=conversation-a";
        const NextPage = () => {
            const dispatch = useDispatch<AppDispatch>();
            return <button onClick={() => dispatch(workspaceOp.changePageOp(2))}>Next page</button>;
        };
        const wrapper = renderWrapper(
            <>
                <ProjectChanges />
                <NextPage />
            </>,
            history,
        );
        await waitFor(() => checkRequestMade(searchUrl, "POST"));
        mockAxios.mockResponseFor({ url: searchUrl }, mockedAxiosResponse({ data: searchAnswer([]) }));
        fireEvent.click(wrapper.getByRole("button", { name: "Next page" }));
        await waitFor(() => checkRequestMade(searchUrl, "POST", { offset: 25 }, true));
        expect(new URLSearchParams(history.location.search).get("highlightConversation")).toBe("conversation-a");
        expect(mockAxios.getReqMatching({ url: searchUrl })?.data).not.toHaveProperty("preservedQueryKeys");
        mockAxios.mockResponseFor({ url: searchUrl }, mockedAxiosResponse({ data: searchAnswer([]) }));
        wrapper.rerender(<NextPage />);
        fireEvent.click(wrapper.getByRole("button", { name: "Next page" }));
        expect(new URLSearchParams(history.location.search).has("highlightConversation")).toBe(false);
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

    it.each([false, true])(
        "keeps a newer search loading when an obsolete request settles (failure: %s)",
        async (failure) => {
            const LoadingState = () => <output>{String(useSelector(workspaceSel.isLoadingSelector))}</output>;
            const view = (refreshKey: number) => (
                <>
                    <LoadingState />
                    <ChangeList projectId={PROJECT_ID} refreshKey={refreshKey} />
                </>
            );
            const wrapper = renderWrapper(view(0));
            await waitFor(() => checkRequestMade(searchUrl, "POST"));
            const older = mockAxios.getReqMatching({ url: searchUrl });
            wrapper.rerender(view(1));
            await waitFor(() =>
                expect(mockAxios.queue().filter((request) => request.url === searchUrl)).toHaveLength(2),
            );
            await act(async () => {
                if (failure) {
                    mockAxios.mockError(
                        mockedAxiosError(500, { title: "Server error", detail: "Obsolete search failed." }),
                        older,
                    );
                } else {
                    mockAxios.mockResponse(mockedAxiosResponse({ data: searchAnswer(changes) }), older);
                }
            });
            expect(wrapper.container.querySelector("output")).toHaveTextContent("true");
            expect(wrapper.container.querySelectorAll("tbody tr")).toHaveLength(0);
            await act(async () => {
                mockAxios.mockResponseFor(
                    { url: searchUrl },
                    mockedAxiosResponse({ data: searchAnswer([mappingChange]) }),
                );
            });
            expect(wrapper.container.querySelector("output")).toHaveTextContent("false");
            expect(wrapper.container.querySelectorAll("tbody tr")).toHaveLength(1);
        },
    );

    it("keeps the newest search results and review boundary when an older refresh finishes last", async () => {
        const wrapper = renderWrapper(<ChangeList projectId={PROJECT_ID} />);
        await waitFor(() => checkRequestMade(searchUrl, "POST"));
        const older = mockAxios.getReqMatching({ url: searchUrl });
        wrapper.rerender(<ChangeList projectId={PROJECT_ID} refreshKey={1} />);
        await waitFor(() => expect(mockAxios.queue().filter((request) => request.url === searchUrl)).toHaveLength(2));
        const newer = mockAxios.queue().filter((request) => request.url === searchUrl)[1];
        const newestChange = { ...mappingChange, seq: 50 };
        await act(async () => {
            mockAxios.mockResponse(mockedAxiosResponse({ data: searchAnswer([newestChange]) }), newer);
        });
        await act(async () => {
            mockAxios.mockResponse(mockedAxiosResponse({ data: searchAnswer(changes) }), older);
        });
        expect(wrapper.container.querySelectorAll("tbody tr")).toHaveLength(1);
        clickFoundElement(wrapper, byTestId("changes-mark-reviewed-btn"));
        clickFoundElement(document.body, byTestId("changes-mark-reviewed-confirm-btn"));
        await waitFor(() => checkRequestMade(reviewedUrl, "PUT", { upTo: newestChange.seq }));
    });

    it.each([false, true])("ignores a previous project's late search response (failure: %s)", async (failure) => {
        setUseParams(PROJECT_ID, "");
        const wrapper = renderWrapper(<ProjectChanges />);
        await waitFor(() => checkRequestMade(searchUrl, "POST"));

        setUseParams("otherProject", "");
        wrapper.rerender(<ProjectChanges />);
        const otherSearchUrl = apiUrl("/workspace/projects/otherProject/changes/search");
        await waitFor(() => checkRequestMade(otherSearchUrl, "POST"));
        await act(async () => {
            mockAxios.mockResponseFor(
                { url: otherSearchUrl },
                mockedAxiosResponse({ data: searchAnswer([mappingChange]) }),
            );
        });
        expect(wrapper.container.querySelectorAll("tbody tr")).toHaveLength(1);

        await act(async () => {
            if (failure) {
                mockAxiosResponse(
                    searchUrl,
                    mockedAxiosError(500, { title: "Server error", detail: "Obsolete search failed." }),
                );
            } else {
                mockAxios.mockResponseFor({ url: searchUrl }, mockedAxiosResponse({ data: searchAnswer(changes) }));
            }
        });
        expect(wrapper.container.querySelectorAll("tbody tr")).toHaveLength(1);
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
