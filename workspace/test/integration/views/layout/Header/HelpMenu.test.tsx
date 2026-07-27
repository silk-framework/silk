import React from "react";
import "@testing-library/jest-dom";
import { createMemoryHistory, MemoryHistory } from "history";
import { screen, waitFor, RenderResult } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import mockAxios from "../../../../__mocks__/axios";
import { renderWrapper } from "../../../TestHelper";
import { HelpMenu } from "../../../../../src/app/views/layout/Header/HelpMenu";
import {
    GridBoardResetProvider,
    useRegisterGridBoardReset,
} from "../../../../../src/app/views/shared/GridBoard/GridBoardResetContext";
import { CONTEXT_PATH, SERVE_PATH } from "../../../../../src/app/constants/path";
import { triggerHotkeyHandler } from "../../../../../src/app/views/shared/HotKeyHandler/HotKeyHandler";

// Decouple from Mousetrap: the shortcuts entry only needs to invoke this handler with the bound key.
jest.mock("../../../../../src/app/views/shared/HotKeyHandler/HotKeyHandler", () => ({
    __esModule: true,
    default: () => {},
    triggerHotkeyHandler: jest.fn(),
    Mousetrap: {},
}));

describe("HelpMenu", () => {
    let wrapper: RenderResult;
    let history: MemoryHistory;

    // Registers a board reset handler (as a mounted GridBoard would) so the "Reset layout" entry appears.
    const BoardRegistrar = ({ reset }: { reset: () => void }) => {
        useRegisterGridBoardReset(reset);
        return null;
    };

    const OVERVIEW_HOTKEY = "shift+?";

    const renderHelpMenu = (options?: { boardReset?: () => void }) => {
        history = createMemoryHistory();
        history.push(`${SERVE_PATH}/projects/SomeProjectId`);
        const ui = (
            <GridBoardResetProvider>
                {options?.boardReset ? <BoardRegistrar reset={options.boardReset} /> : null}
                <HelpMenu />
            </GridBoardResetProvider>
        );
        wrapper = renderWrapper(ui, history, {
            common: { initialSettings: { hotKeys: { overview: OVERVIEW_HOTKEY } } },
        });
        return wrapper;
    };

    const openMenu = async () => {
        const user = userEvent.setup();
        await user.click(screen.getByRole("button", { name: "Help" }));
        // Wait until the portal-rendered menu is present.
        await screen.findByRole("menuitem", { name: "Keyboard shortcuts" });
        return user;
    };

    afterEach(() => {
        wrapper?.unmount();
        (triggerHotkeyHandler as jest.Mock).mockClear();
        mockAxios.reset();
    });

    it("should navigate to the deprecated-plugins SPA route via the router (no full reload)", async () => {
        renderHelpMenu();
        const user = await openMenu();

        await user.click(screen.getByRole("menuitem", { name: "Deprecated plugins usage" }));

        await waitFor(() => {
            expect(history.location.pathname).toBe(`${SERVE_PATH}/deprecatedPlugins`);
        });
    });

    it("should render the API doc entry as a hard full-page <a> link", async () => {
        renderHelpMenu();
        await openMenu();

        const apiItem = screen.getByRole("menuitem", { name: "API" });
        expect(apiItem.tagName).toBe("A");
        expect(apiItem).toHaveAttribute("href", `${CONTEXT_PATH}/doc/api`);
        // A hard navigation link, not a new-tab/window opener.
        expect(apiItem).not.toHaveAttribute("target");
    });

    it("should trigger the keyboard-shortcuts overview hotkey when the shortcuts entry is clicked", async () => {
        renderHelpMenu();
        const user = await openMenu();

        await user.click(screen.getByRole("menuitem", { name: "Keyboard shortcuts" }));

        expect(triggerHotkeyHandler).toHaveBeenCalledTimes(1);
        expect(triggerHotkeyHandler).toHaveBeenCalledWith(OVERVIEW_HOTKEY);
    });

    it("should hide the reset-layout entry when no GridBoard is registered", async () => {
        renderHelpMenu();
        await openMenu();

        expect(screen.queryByRole("menuitem", { name: "Reset layout" })).not.toBeInTheDocument();
    });

    it("should invoke the registered GridBoard reset when the reset-layout entry is clicked", async () => {
        const boardReset = jest.fn();
        renderHelpMenu({ boardReset });
        const user = await openMenu();

        await user.click(screen.getByRole("menuitem", { name: "Reset layout" }));

        expect(boardReset).toHaveBeenCalledTimes(1);
    });
});
