import { act, render } from "@testing-library/react";
import { createMemoryHistory } from "history";
import React from "react";
import { Router } from "react-router-dom";

import RouterOutlet from "./RouterOutlet";

jest.mock("./views/layout/AppLayout/AppLayout", () => {
    const react = jest.requireActual("react") as typeof React;
    return {
        AppLayout: ({ children }: { children: React.ReactNode }) =>
            react.createElement("div", { "data-testid": "persistent-app-layout" }, children),
    };
});

jest.mock("./views/shared/Loading", () => () => null);

jest.mock("react-i18next", () => ({
    useTranslation: () => [(key: string, fallback: string) => fallback || key],
}));

jest.mock("@eccenca/gui-elements", () => {
    const react = jest.requireActual("react") as typeof React;
    return {
        ApplicationContainer: ({ children }: { children: React.ReactNode }) =>
            react.createElement("div", { "data-testid": "application-container" }, children),
        ApplicationContent: ({ children }: { children: React.ReactNode }) => react.createElement("div", null, children),
    };
});

const FirstPage = () => <div>First page</div>;
const SecondPage = () => <div>Second page</div>;

describe("RouterOutlet", () => {
    it("keeps the application layout mounted when navigating between workspace routes", () => {
        const history = createMemoryHistory({ initialEntries: ["/workbench/first"] });
        const view = render(
            <Router history={history}>
                <RouterOutlet
                    routes={[
                        { component: FirstPage, exact: true, path: "/first" },
                        { component: SecondPage, exact: true, path: "/second" },
                    ]}
                />
            </Router>,
        );
        const initialLayout = view.getByTestId("persistent-app-layout");

        act(() => history.push("/workbench/second"));

        expect(view.getByText("Second page")).toBeInTheDocument();
        expect(view.getByTestId("persistent-app-layout")).toBe(initialLayout);
    });

    it("renders component-only routes outside the application layout", () => {
        const history = createMemoryHistory({ initialEntries: ["/workbench/standalone"] });
        const view = render(
            <Router history={history}>
                <RouterOutlet routes={[{ component: FirstPage, exact: true, path: "/standalone", componentOnly: true }]} />
            </Router>,
        );

        expect(view.getByText("First page")).toBeInTheDocument();
        expect(view.queryByTestId("persistent-app-layout")).not.toBeInTheDocument();
        expect(view.getByTestId("application-container")).toBeInTheDocument();
    });
});
