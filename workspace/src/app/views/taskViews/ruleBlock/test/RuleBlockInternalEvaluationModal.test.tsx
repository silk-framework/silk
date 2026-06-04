import React from "react";
import "@testing-library/jest-dom";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import jestTestUtils from "../../../../test/jestTestUtils";
import ruleTestHelper from "../../shared/rules/tests/ruleTestHelper";
import type { IRuleBlockInputExample, RuleBlockSnapshot, RuleBlockPort } from "../ruleBlock.types";

const createInternalEvaluationModalHarness = () => {
    jest.resetModules();
    jest.doMock("react", () => React);

    const mockRequestRuleBlockEvaluation = jest.fn();

    jestTestUtils.mockReactI18next(jestTestUtils.testTranslate);
    jest.doMock("@eccenca/gui-elements", () => ({
        IconButton: ({ text, onClick, ...props }) => (
            <button type="button" onClick={onClick} {...props}>
                {text}
            </button>
        ),
        Notification: ({ children }) => <div data-testid="notification">{children}</div>,
        modalPreventEvents: {},
    }));
    jest.doMock("../ruleBlock.requests", () => ({
        requestRuleBlockEvaluation: (...args) => mockRequestRuleBlockEvaluation(...args),
    }));
    jest.doMock("../../../shared/Loading", () => ({
        __esModule: true,
        default: () => <div data-testid="loading">Loading</div>,
    }));
    jest.doMock("../../../shared/RuleEditor/view/components/RuleEditorBaseModal", () => ({
        RuleEditorBaseModal: ({ children, title, size, headerOptions }) => (
            <div data-testid="internal-evaluation-modal" data-size={size}>
                <div>{title}</div>
                <div>{headerOptions}</div>
                {children}
            </div>
        ),
    }));
    jest.doMock("../RuleBlockEditor", () => {
        const React = require("react");
        const RuleBlockEditorOptionalContext = React.createContext(undefined);
        return {
            __esModule: true,
            RuleBlockEditorOptionalContext,
            RuleBlockEditor: () => {
                const optionalContext = React.useContext(RuleBlockEditorOptionalContext);
                const {
                    RuleBlockEvaluationOptionalContext,
                } = require("../RuleBlockEvaluationOptionalContext") as typeof import("../RuleBlockEvaluationOptionalContext");
                const evaluationContext = React.useContext(RuleBlockEvaluationOptionalContext);
                return (
                    <div
                        data-testid="rule-block-editor"
                        data-label={optionalContext?.ruleBlockLabel ?? ""}
                        data-show-rule-only={String(optionalContext?.showRuleOnly)}
                        data-results={JSON.stringify(evaluationContext.externalEvaluationResults ?? null)}
                    />
                );
            },
        };
    });

    const { RuleBlockInternalEvaluationModal } =
        require("../RuleBlockInternalEvaluationModal") as typeof import("../RuleBlockInternalEvaluationModal");

    return {
        RuleBlockInternalEvaluationModal,
        mockRequestRuleBlockEvaluation,
    };
};

const createSnapshot = (): RuleBlockSnapshot =>
    ruleTestHelper.createRuleBlockInspectionSnapshot({
        ports: [
            ruleTestHelper.createRuleBlockPort({
                id: "labelInput",
                label: "Label",
            }) satisfies RuleBlockPort,
        ],
        operatorTree: ruleTestHelper.createTransformInput({
            id: "normalizeInput",
            inputs: [
                ruleTestHelper.createInputPortInput({
                    id: "labelPortInput",
                    portId: "labelInput",
                }),
            ],
        }),
    });

const createInputExamples = (): IRuleBlockInputExample[] => [
    ruleTestHelper.createRuleBlockInputExample({
        id: "inspection-example-1",
        inputs: {
            labelInput: ["Kiel"],
        },
    }),
];

describe("RuleBlockInternalEvaluationModal", () => {
    it("should request the rule block evaluation, show the loading state, and render the loaded internal evaluation editor with title and fullscreen toggle", async () => {
        const harness = createInternalEvaluationModalHarness();
        const snapshot = createSnapshot();
        const inputExamples = createInputExamples();
        let resolveRequest: (value: unknown) => void = () => undefined;
        harness.mockRequestRuleBlockEvaluation.mockReturnValue(
            new Promise((resolve) => {
                resolveRequest = resolve;
            }),
        );

        render(
            <harness.RuleBlockInternalEvaluationModal
                projectId="project1"
                ruleBlockId="normalizeName"
                snapshot={snapshot}
                inputExamples={inputExamples}
                ruleBlockLabel="Normalize Name"
                onClose={jest.fn()}
            />,
        );

        expect(harness.mockRequestRuleBlockEvaluation).toHaveBeenCalledWith("project1", "normalizeName", {
            ports: snapshot.ports,
            inputExamples,
            operatorTree: snapshot.operatorTree,
            layout: snapshot.layout,
            uiAnnotations: snapshot.uiAnnotations,
        });
        expect(screen.getByTestId("internal-evaluation-modal")).toHaveAttribute("data-size", "xlarge");
        expect(screen.getByText("Evaluation of Normalize Name")).toBeInTheDocument();
        expect(screen.getByTestId("loading")).toBeInTheDocument();

        // The modal should keep the already requested evaluation and only change its container size when toggling
        // fullscreen, then render the injected read-only editor once the evaluation arrives.
        fireEvent.click(screen.getByRole("button", { name: "Maximize" }));
        expect(screen.getByTestId("internal-evaluation-modal")).toHaveAttribute("data-size", "fullscreen");

        resolveRequest({
            data: [
                {
                    operatorId: "normalizeInput",
                    values: ["kiel"],
                    error: null,
                    children: [],
                },
            ],
        });

        await waitFor(() => expect(screen.getByTestId("rule-block-editor")).toBeInTheDocument());
        expect(screen.getByTestId("rule-block-editor")).toHaveAttribute("data-label", "Normalize Name");
        expect(screen.getByTestId("rule-block-editor")).toHaveAttribute("data-show-rule-only", "true");
        expect(screen.getByTestId("rule-block-editor")).toHaveAttribute(
            "data-results",
            JSON.stringify([
                {
                    operatorId: "normalizeInput",
                    values: ["kiel"],
                    error: null,
                    children: [],
                },
            ]),
        );

        fireEvent.click(screen.getByRole("button", { name: "Minimize" }));
        expect(screen.getByTestId("internal-evaluation-modal")).toHaveAttribute("data-size", "xlarge");
    });
});
