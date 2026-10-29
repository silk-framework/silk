import ruleTestHelper from "../../../shared/rules/tests/ruleTestHelper";
import { getLinkRuleInputPaths, getOperatorLabel } from "./LinkingEvaluationViewUtils";

describe("LinkingEvaluationViewUtils", () => {
    it("extracts input paths from rule block bindings and labels rule block inputs", () => {
        const sourceRuleBlock = ruleTestHelper.createRuleBlockInput({
            id: "sourceRuleBlockUsage",
            ruleBlockId: "normalizeName",
            bindings: [
                {
                    portId: "inputPortA",
                    input: ruleTestHelper.createPathInput({
                        id: "sourceBoundPath",
                        path: "name",
                    }),
                },
            ],
        });

        const comparison = ruleTestHelper.createComparisonOperator({
            sourceInput: sourceRuleBlock,
            targetInput: ruleTestHelper.createPathInput({
                id: "targetPath",
                path: "label",
            }),
        });

        expect(getLinkRuleInputPaths(comparison)).toEqual({
            source: {
                name: "sourceBoundPath",
            },
            target: {
                label: "targetPath",
            },
        });
        expect(getOperatorLabel(sourceRuleBlock, [], "<empty>", { normalizeName: "Normalize Name" })).toBe(
            "Normalize Name",
        );
    });
});
