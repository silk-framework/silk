/**
 * Tests for the history.block → custom confirmation surface bridge. The one-shot handler
 * semantics are what keeps the ProjectTaskTabView PromptModal from hijacking confirmations of
 * unrelated `history.block` callers (which must keep the native `window.confirm`).
 */
import { waitFor } from "@testing-library/react";
import {
    getUserConfirmation,
    routeNextBlockConfirmation,
} from "../../../../../src/app/views/shared/projectTaskTabView/unsavedChangesConfirmation";

describe("unsavedChangesConfirmation", () => {
    let confirmSpy: jest.SpyInstance;

    beforeEach(() => {
        confirmSpy = jest.spyOn(window, "confirm").mockReturnValue(true);
    });

    afterEach(() => {
        confirmSpy.mockRestore();
    });

    it("should fall back to window.confirm when no handler is registered", () => {
        const callback = jest.fn();
        getUserConfirmation("message", callback);
        expect(confirmSpy).toHaveBeenCalledWith("message");
        expect(callback).toHaveBeenCalledWith(true);
        confirmSpy.mockReturnValue(false);
        getUserConfirmation("message", callback);
        expect(callback).toHaveBeenLastCalledWith(false);
    });

    it("should consume a registered handler for exactly one confirmation", async () => {
        const handler = jest.fn().mockResolvedValue(false);
        routeNextBlockConfirmation(handler);
        const callback = jest.fn();
        getUserConfirmation("message", callback);
        expect(handler).toHaveBeenCalledTimes(1);
        await waitFor(() => expect(callback).toHaveBeenCalledWith(false));
        expect(confirmSpy).not.toHaveBeenCalled();
        // The handler is one-shot: the next confirmation falls back to window.confirm again.
        const secondCallback = jest.fn();
        getUserConfirmation("message", secondCallback);
        expect(confirmSpy).toHaveBeenCalledTimes(1);
        expect(secondCallback).toHaveBeenCalledWith(true);
        expect(handler).toHaveBeenCalledTimes(1);
    });

    it("should cancel the navigation when the handler rejects", async () => {
        routeNextBlockConfirmation(() => Promise.reject(new Error("handler failure")));
        const callback = jest.fn();
        getUserConfirmation("message", callback);
        await waitFor(() => expect(callback).toHaveBeenCalledWith(false));
        expect(confirmSpy).not.toHaveBeenCalled();
    });
});
