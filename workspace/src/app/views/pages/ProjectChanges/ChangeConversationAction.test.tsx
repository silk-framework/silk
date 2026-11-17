import { openSelectedConversation } from "./ChangeConversationAction";

it("reloads when the requested conversation is already in the URL, so the shared chat mounts again", () => {
    const event = new MouseEvent("click", { cancelable: true });
    const location = { search: "?textQuery=persons&conversationId=chat%201", reload: jest.fn() };
    openSelectedConversation(event, location, "chat 1");
    expect(event.defaultPrevented).toBe(true);
    expect(location.reload).toHaveBeenCalledTimes(1);
    expect(location.search).toBe("?textQuery=persons&conversationId=chat%201");
});

it("lets the link navigate normally when another conversation is requested", () => {
    const event = new MouseEvent("click", { cancelable: true });
    const location = { search: "?conversationId=previous", reload: jest.fn() };
    openSelectedConversation(event, location, "next");
    expect(event.defaultPrevented).toBe(false);
    expect(location.reload).not.toHaveBeenCalled();
});

it.each([{ button: 1 }, { ctrlKey: true }, { metaKey: true }, { shiftKey: true }, { altKey: true }])(
    "preserves the browser's modified-click behavior for %j",
    (modifiers) => {
        const event = new MouseEvent("click", { cancelable: true, ...modifiers });
        const location = { search: "?conversationId=selected", reload: jest.fn() };
        openSelectedConversation(event, location, "selected");
        expect(event.defaultPrevented).toBe(false);
        expect(location.reload).not.toHaveBeenCalled();
    },
);

it.each(["previous", "selected"])("selects live without reloading when the URL names %s", (current) => {
    const event = new MouseEvent("click", { cancelable: true });
    const location = { search: `?conversationId=${current}`, reload: jest.fn() };
    const open = jest.fn();
    openSelectedConversation(event, location, "selected", open);
    expect(event.defaultPrevented).toBe(true);
    expect(open).toHaveBeenCalledWith("selected");
    expect(location.reload).not.toHaveBeenCalled();
});

it.each([{ button: 1 }, { ctrlKey: true }, { metaKey: true }, { shiftKey: true }, { altKey: true }])(
    "preserves modified clicks with a live host for %j",
    (modifiers) => {
        const event = new MouseEvent("click", { cancelable: true, ...modifiers });
        const location = { search: "?conversationId=selected", reload: jest.fn() };
        const open = jest.fn();
        openSelectedConversation(event, location, "selected", open);
        expect(event.defaultPrevented).toBe(false);
        expect(open).not.toHaveBeenCalled();
        expect(location.reload).not.toHaveBeenCalled();
    },
);
