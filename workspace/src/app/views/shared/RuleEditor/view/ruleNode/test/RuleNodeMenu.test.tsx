import React from "react";
import { fireEvent, render, waitFor } from "@testing-library/react";
import { RuleNodeMenu } from "../RuleNodeMenu";
import { RuleEditorUiContext, ruleEditorUiContextDefaultValue } from "../../../contexts/RuleEditorUiContext";
import {
    RuleEditorEvaluationContext,
    RuleEditorEvaluationContextProps,
    ruleEditorEvaluationContextDefaultValue,
} from "../../../contexts/RuleEditorEvaluationContext";
import {
    RuleEditorModelContext,
    RuleEditorModelContextProps,
    ruleEditorModelContextDefaultValue,
} from "../../../contexts/RuleEditorModelContext";
import { RuleEditorContext, ruleEditorContextDefaultValue } from "../../../contexts/RuleEditorContext";
import type { IRuleOperatorNode } from "../../../RuleEditor.typings";
import ruleTestHelper from "../../../../../taskViews/shared/rules/tests/ruleTestHelper";
import { taskUrl } from "../../../../../../store/ducks/router/operations";

jest.mock("@eccenca/gui-elements/src/extensions/react-flow/nodes/NodeTools", () => {
    const React = require("react");
    return {
        __esModule: true,
        NodeTools: ({ children }: { children: React.ReactNode }) => React.createElement("div", null, children),
    };
});

const translation = (translationKey: string, defaultValue?: string) => defaultValue ?? translationKey;

const createRuleBlockNode = (): IRuleOperatorNode =>
    ruleTestHelper.createRuleOperatorNode({
        nodeId: "ruleBlockUsage",
        pluginType: "RuleBlock",
        pluginId: "normalizeName",
        label: "Normalize Name",
    });

const createMenuUi = (
    evaluationContextOverrides: Partial<RuleEditorEvaluationContextProps> = {},
    ruleNode: IRuleOperatorNode = createRuleBlockNode(),
    setCurrentRuleNodeInfo = jest.fn(),
) => {
    const evaluationContext: RuleEditorEvaluationContextProps = {
        ...ruleEditorEvaluationContextDefaultValue,
        evaluationResultsShown: true,
        canEvaluateRuleBlock: () => true,
        openInternalRuleBlockEvaluation: jest.fn(),
        ...evaluationContextOverrides,
    };
    const modelContext: RuleEditorModelContextProps = {
        ...ruleEditorModelContextDefaultValue,
        elements: [],
        ruleOperatorNodes: () => [ruleNode],
    };

    return (
        <RuleEditorUiContext.Provider value={{ ...ruleEditorUiContextDefaultValue, setCurrentRuleNodeInfo }}>
            <RuleEditorEvaluationContext.Provider value={evaluationContext}>
                <RuleEditorModelContext.Provider value={modelContext}>
                    <RuleEditorContext.Provider value={{ ...ruleEditorContextDefaultValue, projectId: "project1" }}>
                        <RuleNodeMenu
                            nodeId={ruleNode.nodeId}
                            t={translation}
                            handleDeleteNode={jest.fn()}
                            handleCloneNode={jest.fn()}
                            ruleOperatorLabel={ruleNode.label}
                            ruleOperatorDescription={ruleNode.description}
                            ruleOperatorDocumentation={ruleNode.markdownDocumentation}
                        />
                    </RuleEditorContext.Provider>
                </RuleEditorModelContext.Provider>
            </RuleEditorEvaluationContext.Provider>
        </RuleEditorUiContext.Provider>
    );
};

const renderMenu = (
    evaluationContextOverrides: Partial<RuleEditorEvaluationContextProps> = {},
    ruleNode: IRuleOperatorNode = createRuleBlockNode(),
    setCurrentRuleNodeInfo = jest.fn(),
) => render(createMenuUi(evaluationContextOverrides, ruleNode, setCurrentRuleNodeInfo));

const internalEvaluationMenuButton = () =>
    document.querySelector('[data-test-id="rule-node-open-internal-rule-block-evaluation-btn"]');
const openRuleBlockMenuButton = () => document.querySelector('[data-test-id="rule-node-open-rule-block-btn"]');

describe("RuleNodeMenu", () => {
    beforeEach(() => {
        jest.spyOn(window, "open").mockImplementation(() => null);
    });

    afterEach(() => {
        jest.restoreAllMocks();
    });

    it("only shows the internal evaluation entry while evaluation results are shown", () => {
        const ruleNode = createRuleBlockNode();
        const { rerender } = renderMenu(
            {
                evaluationResultsShown: false,
            },
            ruleNode,
        );

        expect(internalEvaluationMenuButton()).not.toBeInTheDocument();

        rerender(
            createMenuUi(
                {
                    evaluationResultsShown: true,
                    canEvaluateRuleBlock: () => false,
                },
                ruleNode,
            ),
        );

        expect(internalEvaluationMenuButton()).toHaveAttribute("aria-disabled", "true");
    });

    it("opens the referenced rule block in a new tab from the menu", () => {
        renderMenu();

        fireEvent.click(openRuleBlockMenuButton() as Element);

        expect(window.open).toHaveBeenCalledWith(
            taskUrl("project1", "RuleBlock", "normalizeName"),
            "_blank",
            "noopener",
        );
    });

    it("opens the internal evaluation from the menu when the rule block is evaluable", async () => {
        const openInternalRuleBlockEvaluation = jest.fn();

        renderMenu({
            openInternalRuleBlockEvaluation,
        });

        fireEvent.click(internalEvaluationMenuButton() as Element);

        await waitFor(() =>
            expect(openInternalRuleBlockEvaluation).toHaveBeenCalledWith(
                "ruleBlockUsage",
                "normalizeName",
                "Normalize Name",
            ),
        );
    });

    it("opens documentation with related plugin information", () => {
        const setCurrentRuleNodeInfo = jest.fn();
        const ruleNode = {
            ...createRuleBlockNode(),
            description: "Normalizes a name.",
            markdownDocumentation: "# Normalize name",
            relatedPlugins: [{ id: "trim", description: "Removes leading and trailing whitespace." }],
        };

        renderMenu({}, ruleNode, setCurrentRuleNodeInfo);

        fireEvent.click(document.querySelector('[data-test-id="rule-node-info"]') as Element);

        expect(setCurrentRuleNodeInfo).toHaveBeenCalledWith(
            expect.objectContaining({
                key: "normalizeName",
                title: "Normalize Name",
                relatedPlugins: [
                    expect.objectContaining({
                        plugin: expect.objectContaining({ key: "trim", title: "trim" }),
                        description: "Removes leading and trailing whitespace.",
                    }),
                ],
            }),
        );
    });
});
