import React from "react";
import { act, fireEvent, render, screen, waitFor } from "@testing-library/react";
import RuleEditor, { RuleOperatorFetchFnType } from "../RuleEditor";
import { IRuleOperator, IRuleOperatorNode } from "../RuleEditor.typings";

interface TestRule {
    version: string;
}

interface TestOperator {
    pluginId: string;
}

interface RuleEditorInitializationSnapshot {
    taskVersion?: string;
    nodeVersion?: string;
}

const mockInitializationSnapshots: RuleEditorInitializationSnapshot[] = [];

jest.mock("../model/RuleEditorModel", () => ({
    RuleEditorModel: ({ children }: React.PropsWithChildren) => children,
}));

jest.mock("../view/RuleEditorView", () => ({
    RuleEditorView: () => {
        const ReactModule = require("react") as typeof React;
        const ruleEditorContextModule =
            require("../contexts/RuleEditorContext") as typeof import("../contexts/RuleEditorContext");
        const context = ReactModule.useContext(ruleEditorContextModule.RuleEditorContext);
        const taskVersion = (context.editedItem as TestRule | undefined)?.version;
        const nodeVersion = context.initialRuleOperatorNodes?.[0]?.label;

        ReactModule.useEffect(() => {
            mockInitializationSnapshots.push({ taskVersion, nodeVersion });
        }, [nodeVersion, taskVersion]);

        return ReactModule.createElement("button", { onClick: () => context.saveRule([], []) }, "Save");
    },
}));

const operator: IRuleOperator = {
    pluginType: "unknown",
    pluginId: "operator",
    label: "Operator",
    portSpecification: {
        type: "count",
        minInputPorts: 0,
    },
    parameterSpecification: {},
    tags: [],
    inputsCanBeSwitched: false,
};

const convertToRuleOperatorNodes = (rule: TestRule, _ruleOperator: RuleOperatorFetchFnType): IRuleOperatorNode[] => [
    {
        pluginType: "unknown",
        pluginId: "operator",
        nodeId: "node",
        label: rule.version,
        portSpecification: {
            type: "count",
            minInputPorts: 0,
        },
        parameters: {},
        inputs: [],
        inputsCanBeSwitched: false,
    },
];

describe("RuleEditor", () => {
    beforeEach(() => {
        mockInitializationSnapshots.length = 0;
    });

    it("keeps refreshed task data and its initial rule nodes in the same version", async () => {
        const fetchRuleData = jest
            .fn<Promise<TestRule>, [string, string]>()
            .mockResolvedValueOnce({ version: "original" })
            .mockResolvedValueOnce({ version: "saved" })
            .mockResolvedValueOnce({ version: "external" });

        const ruleEditor = (taskId: string) => (
            <RuleEditor<TestRule, TestOperator>
                projectId="project"
                taskId={taskId}
                fetchRuleData={fetchRuleData}
                saveRule={() => ({ success: true })}
                fetchRuleOperators={() => [{ pluginId: "operator" }]}
                convertRuleOperator={() => operator}
                convertToRuleOperatorNodes={convertToRuleOperatorNodes}
                validateConnection={() => true}
                partialAutoCompletion={() => async () => undefined}
                saveInitiallyEnabled={false}
                showRuleOnly={false}
                instanceId="rule-editor-test"
            />
        );
        const { rerender } = render(ruleEditor("task"));

        await waitFor(() =>
            expect(mockInitializationSnapshots).toContainEqual({
                taskVersion: "original",
                nodeVersion: "original",
            }),
        );

        await act(async () => {
            fireEvent.click(screen.getByRole("button", { name: "Save" }));
        });

        await waitFor(() => expect(fetchRuleData).toHaveBeenCalledTimes(2));
        await waitFor(() =>
            expect(mockInitializationSnapshots).toContainEqual({
                taskVersion: "saved",
                nodeVersion: "saved",
            }),
        );
        expect(mockInitializationSnapshots).not.toContainEqual({
            taskVersion: "saved",
            nodeVersion: "original",
        });

        rerender(ruleEditor("other-task"));

        await waitFor(() => expect(fetchRuleData).toHaveBeenCalledTimes(3));
        await waitFor(() =>
            expect(mockInitializationSnapshots).toContainEqual({
                taskVersion: "external",
                nodeVersion: "external",
            }),
        );
        expect(mockInitializationSnapshots).not.toContainEqual({
            taskVersion: "external",
            nodeVersion: "saved",
        });
    });
});
