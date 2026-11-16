import React from "react";
import { act, render } from "@testing-library/react";
import { AxiosResponse } from "axios";
import { FetchResponse } from "../../../../services/fetch/responseInterceptor";
import { RuleEditorProps } from "../../../shared/RuleEditor/RuleEditor";
import ruleTestHelper from "../../shared/rules/tests/ruleTestHelper";
import { TransformRuleEditor } from "../TransformRuleEditor";
import { TransformRuleEditorOperator } from "../transformEditor.utils";
import { putTransformRule, requestTransformRule } from "../transform.requests";
import {
    AlternativeSaveResult,
    IComplexMappingRule,
    NewComplexMappingRule,
    RuleParameterType,
} from "../transform.types";

type EditorCallbacks = Pick<
    RuleEditorProps<IComplexMappingRule, TransformRuleEditorOperator>,
    "fetchRuleData" | "saveRule"
>;
let mockEditorCallbacks: EditorCallbacks;

jest.mock("../../../shared/RuleEditor/RuleEditor", () => ({
    __esModule: true,
    default: (props: EditorCallbacks) => {
        mockEditorCallbacks = props;
        return null;
    },
}));

jest.mock("../evaluation/TransformRuleEvaluation", () => ({
    __esModule: true,
    default: ({ children }: React.PropsWithChildren) => children,
}));

jest.mock("../../../../hooks/useErrorHandler", () => ({
    __esModule: true,
    default: () => ({ registerError: jest.fn() }),
}));

jest.mock("../transform.requests", () => ({
    requestTransformRule: jest.fn(),
    putTransformRule: jest.fn(),
}));

jest.mock("../transformEditor.utils", () => ({
    __esModule: true,
    default: { inputPathTab: () => ({}), ruleBlockTab: () => ({}) },
}));

const fetchResponse = <T,>(data: T): FetchResponse<T> => new FetchResponse({ data } as AxiosResponse<T>);
const originalRule = ruleTestHelper.createComplexMappingRule();
const changedPathNode = (path: string) =>
    ruleTestHelper.createRuleOperatorNode({
        nodeId: "sourcePath",
        pluginType: "PathInputOperator",
        pluginId: "sourcePathInput",
        parameters: { path },
    });

const editor = (ruleDefinition: RuleParameterType, projectId = "project", taskId = "task") => (
    <TransformRuleEditor
        projectId={projectId}
        transformTaskId={taskId}
        containerRuleId="root"
        instanceId="alternative-save-test"
        ruleDefinition={ruleDefinition}
    />
);

describe("TransformRuleEditor alternative saves", () => {
    beforeEach(() => {
        jest.resetAllMocks();
        jest.mocked(requestTransformRule).mockResolvedValue(fetchResponse(originalRule));
    });

    it.each(["reference", "inline"] as const)("reloads the latest locally saved %s rule", async (kind) => {
        const alternativeSave = jest.fn((rule: NewComplexMappingRule) => ({ ruleToCache: rule }));
        const definition: RuleParameterType =
            kind === "reference"
                ? { ruleId: originalRule.id, alternativeSave }
                : { rule: originalRule, alternativeSave, saveInitiallyEnabled: false };
        render(editor(definition));

        const initial = await mockEditorCallbacks.fetchRuleData("project", "task");
        expect(initial).toEqual(originalRule);
        await act(async () => {
            expect(await mockEditorCallbacks.saveRule([changedPathNode("changed")], [], initial!)).toMatchObject({
                success: true,
            });
        });
        const savedRule = await mockEditorCallbacks.fetchRuleData("project", "task");
        expect(savedRule).toEqual(alternativeSave.mock.calls[0][0]);

        await act(async () => {
            await mockEditorCallbacks.saveRule([changedPathNode("changedAgain")], [], savedRule!);
        });
        expect(await mockEditorCallbacks.fetchRuleData("project", "task")).toEqual(alternativeSave.mock.calls[1][0]);
        expect(requestTransformRule).toHaveBeenCalledTimes(kind === "reference" ? 1 : 0);
        expect(putTransformRule).not.toHaveBeenCalled();
    });

    it("caches the accepted rule returned by the callback instead of the submitted rule", async () => {
        const acceptedRule = ruleTestHelper.createComplexMappingRule();
        const alternativeSave = jest.fn().mockResolvedValue({ ruleToCache: acceptedRule });
        render(editor({ ruleId: originalRule.id, alternativeSave }));

        await act(async () => {
            await mockEditorCallbacks.saveRule([changedPathNode("submitted")], [], originalRule);
        });
        expect(alternativeSave.mock.calls[0][0]).not.toEqual(acceptedRule);
        expect(await mockEditorCallbacks.fetchRuleData("project", "task")).toBe(acceptedRule);
        expect(requestTransformRule).not.toHaveBeenCalled();
    });

    it.each([undefined, {}])("does not cache a callback result of %p", async (result) => {
        const alternativeSave = jest.fn().mockReturnValue(result);
        render(editor({ ruleId: originalRule.id, alternativeSave }));

        await act(async () => {
            expect(await mockEditorCallbacks.saveRule([changedPathNode("changed")], [], originalRule)).toMatchObject({
                success: true,
            });
        });
        expect(await mockEditorCallbacks.fetchRuleData("project", "task")).toEqual(originalRule);
        expect(requestTransformRule).toHaveBeenCalledTimes(1);
    });

    it.each([undefined, {}])("clears a previous local cache when a subsequent save returns %p", async (result) => {
        const alternativeSave = jest
            .fn()
            .mockResolvedValueOnce({ ruleToCache: originalRule })
            .mockResolvedValueOnce(result);
        render(editor({ ruleId: originalRule.id, alternativeSave }));
        await act(async () => {
            await mockEditorCallbacks.saveRule([changedPathNode("first")], [], originalRule);
        });
        const persistedRule = ruleTestHelper.createComplexMappingRule({ id: "persisted" });
        jest.mocked(requestTransformRule).mockResolvedValue(fetchResponse(persistedRule));
        await act(async () => {
            await mockEditorCallbacks.saveRule([changedPathNode("second")], [], originalRule);
        });
        expect(await mockEditorCallbacks.fetchRuleData("project", "task")).toEqual(persistedRule);
        expect(requestTransformRule).toHaveBeenCalledTimes(1);
    });

    it("updates the reload source only after an alternative save succeeds", async () => {
        let finishSave!: (result: AlternativeSaveResult) => void;
        const pendingSave = new Promise<AlternativeSaveResult>((resolve) => {
            finishSave = resolve;
        });
        const alternativeSave = jest.fn().mockReturnValueOnce(pendingSave);
        const definition: RuleParameterType = { rule: originalRule, alternativeSave, saveInitiallyEnabled: false };
        render(editor(definition));

        const save = mockEditorCallbacks.saveRule([changedPathNode("changed")], [], originalRule);
        expect(await mockEditorCallbacks.fetchRuleData("project", "task")).toEqual(originalRule);
        await act(async () => {
            finishSave({ ruleToCache: alternativeSave.mock.calls[0][0] });
            await save;
        });
        const savedRule = alternativeSave.mock.calls[0][0];
        expect(await mockEditorCallbacks.fetchRuleData("project", "task")).toEqual(savedRule);

        alternativeSave.mockRejectedValueOnce(new Error("Save failed"));
        await act(async () => {
            expect(await mockEditorCallbacks.saveRule([changedPathNode("invalid")], [], savedRule)).toMatchObject({
                success: false,
            });
        });
        expect(await mockEditorCallbacks.fetchRuleData("project", "task")).toEqual(savedRule);
    });

    it("does not reuse a local save for a different rule, project or task", async () => {
        const alternativeSave = jest.fn((rule: NewComplexMappingRule) => ({ ruleToCache: rule }));
        const definition: RuleParameterType = { ruleId: originalRule.id, alternativeSave };
        const { rerender } = render(editor(definition));
        await act(async () => {
            await mockEditorCallbacks.saveRule([changedPathNode("changed")], [], originalRule);
        });

        const otherRule = ruleTestHelper.createComplexMappingRule({ id: "otherRule" });
        jest.mocked(requestTransformRule).mockResolvedValue(fetchResponse(otherRule));
        rerender(editor({ ruleId: otherRule.id, alternativeSave }));
        expect(await mockEditorCallbacks.fetchRuleData("project", "task")).toEqual(otherRule);
        rerender(editor(definition, "otherProject"));
        expect(await mockEditorCallbacks.fetchRuleData("otherProject", "task")).toEqual(otherRule);
        rerender(editor(definition, "project", "otherTask"));
        expect(await mockEditorCallbacks.fetchRuleData("project", "otherTask")).toEqual(otherRule);
    });

    it("continues to reload persisted rules after a normal backend save", async () => {
        const definition: RuleParameterType = { ruleId: originalRule.id, alternativeSave: undefined };
        jest.mocked(putTransformRule).mockResolvedValue(fetchResponse(undefined));
        render(editor(definition));
        await act(async () => {
            expect(await mockEditorCallbacks.saveRule([changedPathNode("changed")], [], originalRule)).toMatchObject({
                success: true,
            });
        });
        const savedRule = jest.mocked(putTransformRule).mock.calls[0][3];
        jest.mocked(requestTransformRule).mockResolvedValue(fetchResponse(savedRule));
        expect(await mockEditorCallbacks.fetchRuleData("project", "task")).toEqual(savedRule);
        expect(requestTransformRule).toHaveBeenCalledWith("project", "task", originalRule.id);
    });
});
