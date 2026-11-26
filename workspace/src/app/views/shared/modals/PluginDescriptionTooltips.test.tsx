import React from "react";
import { CLASSPREFIX, TooltipProps } from "@eccenca/gui-elements";
import { render, screen } from "@testing-library/react";
import { createInstance } from "i18next";
import { I18nextProvider } from "react-i18next";

import "@testing-library/jest-dom";

import en from "../../../../locales/manual/en.json";
import { RuleOperator } from "../RuleEditor/view/sidebar/RuleOperator";
import type { SidebarRuleOperatorBase } from "../RuleEditor/view/sidebar/RuleEditorOperatorSidebar.typings";
import { PluginDocumentationModal } from "./PluginDocumentationModal";

jest.mock("@eccenca/gui-elements/src/components/Tooltip/Tooltip", () => {
    const React = jest.requireActual<typeof import("react")>("react");
    const actual = jest.requireActual<typeof import("@eccenca/gui-elements/src/components/Tooltip/Tooltip")>(
        "@eccenca/gui-elements/src/components/Tooltip/Tooltip",
    );
    function OpenTooltip(props: TooltipProps) {
        return React.createElement(actual.Tooltip, { ...props, isOpen: true, usePortal: false });
    }
    return { __esModule: true, default: OpenTooltip, Tooltip: OpenTooltip };
});

const ruleOperator: SidebarRuleOperatorBase = {
    pluginType: "TransformOperator",
    pluginId: "testOperator",
    label: "Operator",
    tags: [],
    inputsCanBeSwitched: false,
};

const literalDescription = "Matches *input*.\n\nReturns the original value.";
const tooltipSelector = `.${CLASSPREFIX}-tooltip__content`;

describe("Plugin description tooltips", () => {
    it("preserves literal punctuation and the help hint in the documentation icon tooltip", async () => {
        const i18n = createInstance();
        await i18n.init({ lng: "en", resources: { en: { translation: en } }, interpolation: { escapeValue: false } });
        const { container } = render(
            <I18nextProvider i18n={i18n}>
                <RuleOperator
                    ruleOperator={{ ...ruleOperator, description: "Matches *input*.", markdownDocumentation: "# Help" }}
                    searchWords={[]}
                    textQuery=""
                />
            </I18nextProvider>,
        );

        expect(container.querySelector(tooltipSelector)?.textContent).toBe(
            "Matches *input*.\n\nClick to view full documentation",
        );
    });

    it("preserves literal punctuation in a multiline info icon tooltip", () => {
        const { container } = render(
            <RuleOperator
                ruleOperator={{ ...ruleOperator, description: literalDescription }}
                searchWords={[]}
                textQuery=""
            />,
        );

        expect(container.querySelector(tooltipSelector)?.textContent).toBe(literalDescription);
    });

    it("shows the complete description as plain text in the search snippet tooltip", () => {
        const { container } = render(
            <RuleOperator
                ruleOperator={{ ...ruleOperator, description: literalDescription }}
                searchWords={["input"]}
                textQuery="input"
            />,
        );

        const snippet = container.querySelector('[data-test-id="ruleOperator-sidebar-search-operator-description"]');
        expect(snippet?.querySelector(tooltipSelector)?.textContent).toBe(literalDescription);
    });

    it.each(["RuleBlock", "InputPortOperator"] as const)(
        "renders Markdown for a user-authored %s description",
        (pluginType) => {
            const { container } = render(
                <RuleOperator
                    ruleOperator={{ ...ruleOperator, pluginType, description: literalDescription }}
                    searchWords={[]}
                    textQuery=""
                />,
            );

            expect(container.querySelector(`${tooltipSelector} em`)?.textContent).toBe("input");
        },
    );

    it("shows related plugin descriptions literally alongside rendered documentation", () => {
        const { baseElement } = render(
            <PluginDocumentationModal
                documentationToShow={{
                    key: "plugin",
                    title: "Plugin",
                    markdownDocumentation: "# Operator documentation",
                    relatedPlugins: [{ plugin: { key: "related" }, description: literalDescription }],
                }}
                onClose={jest.fn()}
            />,
        );

        expect(screen.getByRole("heading", { name: "Operator documentation" })).toBeInTheDocument();
        const related = baseElement.querySelector('[data-test-id="related-plugin-related"]');
        expect(related?.querySelector(tooltipSelector)?.textContent).toBe(literalDescription);
    });
});
