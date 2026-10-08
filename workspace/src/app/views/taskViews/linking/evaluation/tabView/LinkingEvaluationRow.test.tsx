import React from "react";
import "@testing-library/jest-dom";
import { act, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { createInstance } from "i18next";
import { I18nextProvider } from "react-i18next";
import { Table, TableBody } from "@eccenca/gui-elements";
import translations from "../../../../../../locales/manual/en.json";
import { LinkingEvaluationRow } from "./LinkingEvaluationRow";

jest.mock("react-redux", () => ({
    ...jest.requireActual("react-redux"),
    useSelector: () => undefined,
    useDispatch: () => jest.fn(),
}));

const i18n = createInstance();

beforeAll(async () => {
    await i18n.init({ lng: "en", resources: { en: { translation: translations } } });
});

it("focuses each link state button once and activates Confirm with Enter", async () => {
    const user = userEvent.setup();
    const handleReferenceLinkTypeUpdate = jest.fn().mockResolvedValue(true);
    const { container } = render(
        <I18nextProvider i18n={i18n}>
            <Table>
                <TableBody>
                    <LinkingEvaluationRow
                        rowIdx={0}
                        colSpan={6}
                        rowIsExpandedByParent={false}
                        linkingEvaluationResult={{
                            source: "source",
                            target: "target",
                            confidence: 0.5,
                            decision: "unlabeled",
                            ruleValues: { operatorId: "rule", score: 0.5, children: [] },
                        }}
                        handleReferenceLinkTypeUpdate={handleReferenceLinkTypeUpdate}
                        searchQuery=""
                        operatorTreeExpandedByDefault={false}
                        inputValuesExpandedByDefault={false}
                        operatorPlugins={[]}
                        ruleBlockLabels={{}}
                        expandedBySearch={false}
                    />
                </TableBody>
            </Table>
        </I18nextProvider>,
    );

    const targetCopyButton = container.querySelector<HTMLElement>('[data-test-id="linking-evaluation-target-copy-0"]');
    expect(targetCopyButton).not.toBeNull();
    targetCopyButton!.focus();

    const confirmButton = container.querySelector<HTMLElement>('[data-test-id="link-state-button-positive"]');
    const uncertainButton = screen.getByRole("button", { name: "Uncertain" });
    const declineButton = screen.getByRole("button", { name: "Decline" });
    expect(confirmButton).not.toBeNull();
    await act(async () => user.tab());
    expect(confirmButton).toHaveFocus();
    expect(confirmButton).toHaveAccessibleName("Confirm");
    expect(confirmButton).toHaveAttribute("aria-pressed", "false");
    expect(uncertainButton).toHaveAttribute("aria-pressed", "true");
    expect(declineButton).toHaveAttribute("aria-pressed", "false");
    await act(async () => user.keyboard("{Enter}"));
    await waitFor(() =>
        expect(handleReferenceLinkTypeUpdate).toHaveBeenCalledWith("unlabeled", "positive", "source", "target", 0),
    );
    await waitFor(() => expect(confirmButton).toHaveAttribute("aria-pressed", "true"));
    expect(uncertainButton).toHaveAttribute("aria-pressed", "false");

    await act(async () => user.tab());
    expect(uncertainButton).toHaveFocus();
    await act(async () => user.tab());
    expect(declineButton).toHaveFocus();
});
