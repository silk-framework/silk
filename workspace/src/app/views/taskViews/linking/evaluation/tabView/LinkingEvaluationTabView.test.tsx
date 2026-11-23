import React from "react";
import { fireEvent, waitFor } from "@testing-library/react";
import { renderWrapper } from "../../../../../../../test/integration/TestHelper";
import LinkingEvaluationTabView from "./LinkingEvaluationTabView";

jest.mock("../../../../shared/TaskActivityWidget/TaskActivityWidget", () => ({ TaskActivityWidget: () => null }));
jest.mock("@ducks/common/requests", () => ({
    ...jest.requireActual("@ducks/common/requests"),
    requestRuleOperatorPluginsDetails: () => Promise.resolve({ data: {} }),
}));

it("announces link filter state after selecting, replacing, and clearing a filter", async () => {
    renderWrapper(<LinkingEvaluationTabView projectId="project" linkingTaskId="linking" />);
    const open = () => {
        const trigger = document.querySelector('[data-test-id="link-state-filter-btn"]');
        if (!trigger) throw new Error("Missing link state filter trigger");
        fireEvent.click(trigger);
    };
    const item = (state: string) => document.querySelector(`[data-test-id="link-${state}-state"]`)!;
    const check = async (state: string, checked: boolean) => {
        await waitFor(() => {
            expect(item(state)).toHaveAttribute("role", "menuitemcheckbox");
            expect(item(state)).toHaveAttribute("aria-checked", String(checked));
        });
    };
    open();
    await check("confirmed", false);
    fireEvent.click(item("confirmed"));
    open();
    await check("confirmed", true);
    fireEvent.click(item("uncertain"));
    open();
    await check("confirmed", false);
    await check("uncertain", true);
    fireEvent.click(item("uncertain"));
    open();
    await check("uncertain", false);
});
