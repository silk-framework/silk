import React from "react";
import "@testing-library/jest-dom";
import { act, render, screen } from "@testing-library/react";

const mockUseNotificationsQueue = jest.fn((_instanceId?: string) => ({
    messages: [],
    notifications: null,
}));

jest.mock("react-i18next", () => ({
    useTranslation: () => [(key) => key],
}));

jest.mock("@eccenca/gui-elements", () => {
    const mockJestTestUtils = require("../../../../../test/jestTestUtils").default;
    return {
        ContextOverlay: mockJestTestUtils.createContextOverlayMock(),
        Icon: mockJestTestUtils.createIconMock(),
        IconButton: mockJestTestUtils.createButtonMock(),
        Notification: mockJestTestUtils.createNotificationMock(),
        Spacing: mockJestTestUtils.createChildrenOnlyMock(),
    };
});

jest.mock("../../../ApplicationNotifications/NotificationsMenu", () => ({
    useNotificationsQueue: (instanceId: string) => mockUseNotificationsQueue(instanceId),
}));

import { RuleEditorNotifications } from "../RuleEditorNotifications";

describe("RuleEditorNotifications", () => {
    beforeEach(() => {
        jest.useFakeTimers();
        mockUseNotificationsQueue.mockReturnValue({
            messages: [],
            notifications: null,
        });
    });

    afterEach(() => {
        jest.runOnlyPendingTimers();
        jest.useRealTimers();
        jest.clearAllMocks();
    });

    it("should auto-close warning-only notifications after five seconds", () => {
        render(
            <RuleEditorNotifications
                saveWarningMessages={["Unused port warning"]}
                queueEditorNotifications={[]}
                queueNodeNotifications={[]}
                nodeJumpToHandler={jest.fn()}
                generalNotificationMinDateTime={0}
            />,
        );

        expect(screen.getByTestId("context-overlay")).toBeInTheDocument();

        act(() => {
            jest.advanceTimersByTime(5000);
        });

        expect(screen.queryByTestId("context-overlay")).not.toBeInTheDocument();
    });

    it("should keep the overlay open when warnings and errors are shown together", () => {
        render(
            <RuleEditorNotifications
                saveWarningMessages={["Unused port warning"]}
                queueEditorNotifications={["Save failed"]}
                queueNodeNotifications={[]}
                nodeJumpToHandler={jest.fn()}
                generalNotificationMinDateTime={0}
            />,
        );

        act(() => {
            jest.advanceTimersByTime(5000);
        });

        expect(screen.getByTestId("context-overlay")).toBeInTheDocument();
        expect(screen.getAllByTestId("notification")).toHaveLength(2);
    });
});
