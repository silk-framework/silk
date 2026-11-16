import React from "react";
import { fireEvent, render, waitFor, within } from "@testing-library/react";
import { ChangeConversationsContext } from "./ChangeConversationsContext";
import { ChangeConversationLabel, ChangeSource } from "./ChangeSource";
import { IChangeEntry } from "./changesRequests";

let mockSearch = "";
jest.mock("react-router", () => ({ useLocation: () => ({ search: mockSearch }) }));
beforeEach(() => {
    mockSearch = "";
});

jest.mock("./ChangeConversationsContext", () => {
    const React = jest.requireActual<typeof import("react")>("react");
    return { ChangeConversationsContext: React.createContext(new Map()) };
});
jest.mock("react-i18next", () => {
    const english = jest.requireActual<typeof import("../../../../locales/manual/en.json")>(
        "../../../../locales/manual/en.json",
    );
    return {
        useTranslation: () => [
            (key: string, values?: { number?: number; id?: string; label?: string }) => {
                switch (key) {
                    case "pages.changes.agentSource":
                        return "AI-origin change";
                    case "pages.changes.originTooltip":
                        return "The client the change came from";
                    case "pages.changes.conversation.badge":
                        return `C${values?.number}`;
                    case "pages.changes.conversation.id":
                        return `ID: ${values?.id}`;
                    case "pages.changes.conversation.label":
                        return english.pages.changes.conversation.label.replace("{{label}}", values?.label ?? "");
                    case "pages.changes.conversation.filter":
                        return `Filter by conversation ${values?.label}`;
                    case "pages.changes.conversation.unavailableBadge":
                        return "?";
                    case "pages.changes.conversation.unavailable":
                        return "Conversation unavailable";
                    case "pages.changes.conversation.untitled":
                        return "Untitled conversation";
                    default:
                        return key;
                }
            },
        ],
    };
});

const entry: IChangeEntry = {
    seq: 1,
    timestamp: "2026-11-03T08:00:00Z",
    origin: "mcp:claude-code",
    conversationId: "conversation-a",
    type: "AddTask",
    kind: "added",
    description: "Added task",
    summary: "Added task",
    details: [],
    links: [],
    revertible: true,
};

it("shows only the conversation badge with its metadata and one focus stop", async () => {
    const select = jest.fn();
    const view = render(
        <ChangeConversationsContext.Provider value={new Map([["conversation-a", { number: 2, title: "Map movies" }]])}>
            <ChangeSource entry={entry} selected onSelect={select} />
        </ChangeConversationsContext.Provider>,
    );
    const conversation = view.getByRole("button", { name: "Filter by conversation C2" });
    expect(view.queryByRole("img", { name: "AI-origin change" })).not.toBeInTheDocument();
    expect(view.container.querySelectorAll("svg")).toHaveLength(1);
    expect(view.container.querySelector("[title]")).toBeNull();
    expect(view.container.querySelectorAll('[tabindex="0"]')).toHaveLength(1);
    expect(within(conversation).queryByRole("img")).toBeNull();

    fireEvent.focus(conversation);
    const metadata = await view.findByRole("tooltip");
    expect(metadata.textContent).toBe("Conversation label: Map movies\n\nID: conversation-a");
    expect(metadata).not.toHaveTextContent("mcp:claude-code");
    expect(conversation).toHaveAccessibleDescription(/Conversation label: Map movies\s+ID: conversation-a/);
    expect(conversation).toHaveAttribute("aria-pressed", "true");
    fireEvent.click(conversation);
    expect(select).toHaveBeenCalledWith("conversation-a");
    fireEvent.keyDown(document, { key: "Escape", keyCode: 27 });
    await waitFor(() => expect(view.queryByRole("tooltip")).not.toBeInTheDocument());
});

it("uses the same AI icon for the conversation badge and the standalone indicator", () => {
    const view = render(
        <>
            <ChangeSource entry={entry} />
            <ChangeSource entry={{ ...entry, seq: 2, conversationId: undefined }} />
        </>,
    );
    const badgeIcon = view.getByRole("button", { name: "Filter by conversation ?" }).querySelector("svg");
    const aiIcon = view.getByRole("img", { name: "AI-origin change" }).querySelector("svg");
    expect(badgeIcon).not.toBeNull();
    expect(aiIcon).not.toBeNull();
    expect(badgeIcon?.innerHTML).toBe(aiIcon?.innerHTML);
});

it.each([undefined, ""])("shows only the AI indicator when the conversation ID is %s", async (conversationId) => {
    const view = render(<ChangeSource entry={{ ...entry, conversationId }} />);
    const ai = view.getByRole("img", { name: "AI-origin change" });
    expect(view.queryByRole("button")).not.toBeInTheDocument();
    expect(view.container.querySelectorAll("svg")).toHaveLength(1);
    expect(view.container.querySelector("[title]")).toBeNull();
    fireEvent.focus(ai);
    const tooltip = await view.findByRole("tooltip");
    expect(tooltip).toHaveTextContent("The client the change came from: mcp:claude-code");
    expect(tooltip).not.toHaveTextContent("conversation-a");
    expect(ai).toHaveAccessibleDescription("The client the change came from: mcp:claude-code");
});

it("keeps unavailable conversations filterable and exposes their ID on hover", async () => {
    const select = jest.fn();
    const view = render(<ChangeSource entry={entry} onSelect={select} />);
    const conversation = view.getByRole("button", { name: "Filter by conversation ?" });
    expect(view.queryByRole("img", { name: "AI-origin change" })).not.toBeInTheDocument();
    fireEvent.mouseEnter(conversation);
    const metadata = await view.findByRole("tooltip");
    expect(metadata).toHaveTextContent("Conversation unavailable");
    expect(metadata).toHaveTextContent("ID: conversation-a");
    expect(metadata).not.toHaveTextContent("mcp:claude-code");
    fireEvent.click(conversation);
    expect(select).toHaveBeenCalledWith("conversation-a");
});

it.each([true, false])(
    "shows a filterable conversation badge without origin (metadata available: %s)",
    async (available) => {
        const select = jest.fn();
        const metadata = new Map(available ? [["conversation-a", { number: 2, title: "Map movies" }]] : []);
        const view = render(
            <ChangeConversationsContext.Provider value={metadata}>
                <ChangeSource entry={{ ...entry, origin: undefined }} onSelect={select} />
            </ChangeConversationsContext.Provider>,
        );
        const badge = view.getByRole("button", { name: `Filter by conversation ${available ? "C2" : "?"}` });
        fireEvent.click(badge);
        expect(select).toHaveBeenCalledWith("conversation-a");
        fireEvent.focus(badge);
        expect(await view.findByRole("tooltip")).toHaveTextContent("ID: conversation-a");
    },
);

it("leaves normal user changes without an origin empty", () => {
    const view = render(<ChangeSource entry={{ ...entry, origin: undefined, conversationId: undefined }} />);
    expect(view.container).toBeEmptyDOMElement();
});

it("highlights the linked ID in Source and the facet without selecting a filter, and responds to URL changes", () => {
    mockSearch = "?highlightConversation=conversation-a";
    const content = () => (
        <>
            <div data-testid="facet">
                <ChangeConversationLabel conversationId="conversation-a" />
            </div>
            <ChangeSource entry={entry} />
        </>
    );
    const view = render(content());
    const button = view.getByRole("button", { name: "Filter by conversation ?" });
    expect(button).toHaveClass("diapp-changes__conversation--highlighted");
    expect(button).toHaveAttribute("aria-current", "true");
    expect(button).toHaveAttribute("aria-pressed", "false");
    expect(view.getByTestId("facet").querySelector(".diapp-changes__conversation--highlighted")).not.toBeNull();
    expect(view.getByTestId("facet").querySelector(".eccgui-tag__item")).not.toHaveClass("eccgui-tag--small");
    expect(view.queryByRole("img", { name: "AI-origin change" })).not.toBeInTheDocument();
    mockSearch = "?highlightConversation=other";
    view.rerender(content());
    expect(view.container.querySelector(".diapp-changes__conversation--highlighted")).toBeNull();
    expect(button).not.toHaveAttribute("aria-current");
});

it("uses the same tooltip for the facet and Source badge, preserving the full label and blank line", async () => {
    const label = "Movie mapping ".repeat(12).trim();
    const view = render(
        <ChangeConversationsContext.Provider value={new Map([["conversation-a", { number: 2, title: label }]])}>
            <div data-testid="facet">
                <ChangeConversationLabel conversationId="conversation-a" />
            </div>
            <ChangeSource entry={entry} />
        </ChangeConversationsContext.Provider>,
    );
    expect(view.container.querySelector("[title]")).toBeNull();
    const facet = within(view.getByTestId("facet")).getByText("C2").closest('[tabindex="0"]');
    expect(facet).not.toBeNull();
    if (!facet) {
        throw new Error("The conversation facet label must be focusable");
    }
    fireEvent.focus(facet);
    const facetTooltip = await view.findByRole("tooltip");
    const expected = `Conversation label: ${label}\n\nID: conversation-a`;
    expect(facetTooltip.textContent).toBe(expected);
    expect(facetTooltip).toHaveStyle({ whiteSpace: "pre-wrap" });
    expect(facet).toHaveAccessibleDescription(new RegExp(`Conversation label: ${label}\\s+ID: conversation-a`));
    fireEvent.blur(facet);
    await waitFor(() => expect(view.queryByRole("tooltip")).not.toBeInTheDocument());
    fireEvent.focus(view.getByRole("button", { name: "Filter by conversation C2" }));
    const sourceTooltip = await view.findByRole("tooltip");
    expect(sourceTooltip.textContent).toBe(expected);
    expect(sourceTooltip).toHaveStyle({ whiteSpace: "pre-wrap" });
});

it("supports removing a numbered conversation tag while retaining its metadata tooltip", async () => {
    const remove = jest.fn();
    const view = render(
        <ChangeConversationsContext.Provider value={new Map([["conversation-a", { number: 2, title: "Map movies" }]])}>
            <ChangeConversationLabel conversationId="conversation-a" onRemove={remove} />
        </ChangeConversationsContext.Provider>,
    );
    expect(view.getByText("C2")).toBeInTheDocument();
    expect(view.container.querySelectorAll(".eccgui-tag__item")).toHaveLength(1);
    const target = view.getByText("C2").closest<HTMLElement>('[tabindex="0"]');
    if (!target) {
        throw new Error("Expected the conversation tooltip target");
    }
    fireEvent.focus(target);
    expect((await view.findByRole("tooltip")).textContent).toBe("Conversation label: Map movies\n\nID: conversation-a");
    fireEvent.click(view.getByRole("button", { name: "Remove tag" }));
    expect(remove).toHaveBeenCalledTimes(1);
});
