import React from "react";
import "@testing-library/jest-dom";
import { render, screen, within } from "@testing-library/react";
import { NotificationsMenu } from "./NotificationsMenu";

jest.mock("react-redux", () => ({
    useDispatch: () => jest.fn(),
    useSelector: (selector) =>
        selector === "errors"
            ? {
                  errors: [
                      { id: "first", message: "First warning", timestamp: 1 },
                      { id: "second", message: "Second warning", timestamp: 2 },
                  ],
              }
            : true,
}));

jest.mock("@ducks/error/selectors", () => ({ __esModule: true, default: "errors" }));
jest.mock("@ducks/common", () => ({
    commonSel: { notificationMenuSelector: "display" },
    commonOp: { toggleNotificationMenuDisplay: jest.fn() },
}));
jest.mock("../../../hooks/useErrorHandler", () => ({ __esModule: true, default: () => ({ clearErrors: jest.fn() }) }));
jest.mock("./ApplicationNotification", () => ({
    ApplicationNotification: ({ errorItem }) => require("react").createElement("div", null, errorItem.message),
}));
jest.mock("@eccenca/gui-elements", () => ({
    ApplicationToolbarAction: ({ children }) => require("react").createElement("button", null, children),
    ApplicationToolbarPanel: ({ children }) => require("react").createElement("section", null, children),
    Badge: () => null,
    Button: ({ text }) => require("react").createElement("button", null, text),
    ContextOverlay: ({ children }) => children,
    Depiction: () => null,
    Divider: () => null,
    Icon: () => null,
    Spacing: () => null,
    useApplicationHeaderOverModals: () => undefined,
}));

test("the notifications panel exposes its messages as a list", () => {
    render(<NotificationsMenu />);

    const list = screen.getByRole("list");
    expect(within(list).getAllByRole("listitem")).toHaveLength(2);
    expect(within(list).getByText("First warning")).toBeInTheDocument();
    expect(within(list).getByText("Second warning")).toBeInTheDocument();
    expect(list).not.toContainElement(screen.getByRole("button", { name: "Clear all messages" }));
});
