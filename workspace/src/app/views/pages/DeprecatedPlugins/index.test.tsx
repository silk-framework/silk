import React from "react";
import { act, render, waitFor } from "@testing-library/react";
import DeprecatedPlugins, { DeprecatedPluginsModel } from "./index";
import {
    ApplicationContextRegistryProvider,
    useApplicationContextRegistry,
} from "../../../contexts/ApplicationContextRegistry";

const mockRequest = jest.fn<Promise<{ data: DeprecatedPluginsModel[] }>, []>();
jest.mock("@ducks/common/requests", () => ({ requestDeprecatedPlugins: () => mockRequest() }));
jest.mock("../../../hooks/useErrorHandler", () => ({
    __esModule: true,
    default: () => ({ registerError: jest.fn() }),
}));
jest.mock("../../../views/shared/PageHeader/PageHeader", () => ({ usePageHeader: () => ({ pageHeader: null }) }));
jest.mock("./DeprecatedPluginsSidebar", () => ({ DeprecatedPluginsSidebar: () => null }));
jest.mock("./DeprecatedPluginsList", () => ({ DeprecatedPluginsList: () => null }));
jest.mock("react-router", () => ({ useHistory: () => ({ replace: jest.fn() }), useLocation: () => ({ search: "" }) }));
jest.mock("react-i18next", () => ({ useTranslation: () => [(key: string) => key] }));

const ContextState = () => {
    const { pageContributions } = useApplicationContextRegistry();
    return <output data-testid="context">{JSON.stringify(pageContributions)}</output>;
};
const mount = () =>
    render(
        <ApplicationContextRegistryProvider>
            <DeprecatedPlugins />
            <ContextState />
        </ApplicationContextRegistryProvider>,
    );

beforeEach(() => mockRequest.mockReset());

it.each([{ rows: [] }, { rows: [{ pluginId: "old", pluginLabel: "Old plugin" }] }])(
    "publishes usage context only after a successful lookup (%j)",
    async ({ rows }) => {
        let complete: ((result: { data: DeprecatedPluginsModel[] }) => void) | undefined;
        mockRequest.mockReturnValueOnce(
            new Promise((resolve) => {
                complete = resolve;
            }),
        );
        const view = mount();
        expect(view.getByTestId("context").textContent).toBe("[]");
        await act(async () => {
            complete?.({ data: rows });
        });
        await waitFor(() =>
            expect(JSON.parse(view.getByTestId("context").textContent ?? "[]")).toEqual([
                expect.objectContaining({ kind: "deprecatedPlugins", totalUsageCount: rows.length }),
            ]),
        );
    },
);

it("does not report zero usages after a failed lookup", async () => {
    let fail: ((error: Error) => void) | undefined;
    mockRequest.mockReturnValueOnce(
        new Promise((_, reject) => {
            fail = reject;
        }),
    );
    const view = mount();
    await act(async () => {
        fail?.(new Error("Server unavailable"));
    });
    expect(view.getByTestId("context").textContent).toBe("[]");
});
