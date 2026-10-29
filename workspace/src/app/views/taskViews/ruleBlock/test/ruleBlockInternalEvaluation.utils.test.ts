import ruleBlockInternalEvaluationUtils from "../ruleBlockInternalEvaluation.utils";
import ruleTestHelper from "../../shared/rules/tests/ruleTestHelper";
import { EvaluatedTransformEntity } from "../../transform/transform.types";
import {
    AggregationConfidence,
    ComparisonConfidence,
    IComparisonOperator,
    IEntityLink,
    ILinkingRule,
} from "../../linking/linking.types";
import { IComplexMappingRule } from "../../transform/transform.types";

describe("ruleBlockInspectionUtils", () => {
    it("should derive rule block input examples from transform black-box evaluation nodes", () => {
        const rule: IComplexMappingRule = ruleTestHelper.createComplexMappingRule({
            operator: ruleTestHelper.createRuleBlockInput({
                id: "rule-block-node",
                ruleBlockId: "normalizeName",
                bindings: [
                    {
                        portId: "portA",
                        input: ruleTestHelper.createPathInput({
                            id: "source-path-a",
                            path: "name",
                        }),
                    },
                    {
                        portId: "portB",
                        input: ruleTestHelper.createPathInput({
                            id: "source-path-b",
                            path: "alias",
                        }),
                    },
                ],
            }),
        });
        const evaluations: EvaluatedTransformEntity[] = [
            {
                operatorId: "root-node",
                values: ["Root value"],
                error: null,
                children: [
                    {
                        operatorId: "rule-block-node",
                        values: ["Rule block output"],
                        error: null,
                        children: [
                            {
                                operatorId: "source-path-a",
                                values: ["Input A"],
                                error: null,
                                children: [],
                            },
                            {
                                operatorId: "source-path-b",
                                values: ["Input B 1", "Input B 2"],
                                error: null,
                                children: [],
                            },
                        ],
                    },
                ],
            },
        ];

        expect(
            ruleBlockInternalEvaluationUtils.createInputExamplesFromTransformEvaluations(
                evaluations,
                "rule-block-node",
                rule.operator,
            ),
        ).toStrictEqual([
            {
                id: "inspection-example-1",
                inputs: {
                    portA: ["Input A"],
                    portB: ["Input B 1", "Input B 2"],
                },
            },
        ]);
    });

    it("should derive multiple transform input examples while ignoring unmatched black-box children", () => {
        const rule: IComplexMappingRule = ruleTestHelper.createComplexMappingRule({
            operator: ruleTestHelper.createTransformInput({
                id: "outer-transform",
                function: "trim",
                inputs: [
                    ruleTestHelper.createRuleBlockInput({
                        id: "rule-block-node",
                        ruleBlockId: "normalizeName",
                        bindings: [
                            {
                                portId: "portA",
                                input: ruleTestHelper.createPathInput({
                                    id: "source-path-a",
                                    path: "name",
                                }),
                            },
                            {
                                portId: "portB",
                                input: ruleTestHelper.createPathInput({
                                    id: "source-path-b",
                                    path: "alias",
                                }),
                            },
                        ],
                    }),
                ],
            }),
        });
        const evaluations: EvaluatedTransformEntity[] = [
            {
                operatorId: "outer-transform",
                values: ["first"],
                error: null,
                children: [
                    {
                        operatorId: "rule-block-node",
                        values: ["Rule block output 1"],
                        error: null,
                        children: [
                            {
                                operatorId: "source-path-a",
                                values: ["Input A1"],
                                error: null,
                                children: [],
                            },
                            {
                                operatorId: "ignored-child",
                                values: ["Ignored"],
                                error: null,
                                children: [],
                            },
                        ],
                    },
                ],
            },
            {
                operatorId: "outer-transform",
                values: ["second"],
                error: null,
                children: [
                    {
                        operatorId: "rule-block-node",
                        values: ["Rule block output 2"],
                        error: null,
                        children: [
                            {
                                operatorId: "source-path-a",
                                values: ["Input A2"],
                                error: null,
                                children: [],
                            },
                            {
                                operatorId: "source-path-b",
                                values: ["Input B2"],
                                error: null,
                                children: [],
                            },
                        ],
                    },
                ],
            },
        ];

        // The first example only binds one matching child, while the second binds both ports. The unmatched child must
        // be ignored so the derived examples reflect only the configured rule block bindings.
        expect(
            ruleBlockInternalEvaluationUtils.createInputExamplesFromTransformEvaluations(
                evaluations,
                "rule-block-node",
                rule.operator,
            ),
        ).toStrictEqual([
            {
                id: "inspection-example-1",
                inputs: {
                    portA: ["Input A1"],
                },
            },
            {
                id: "inspection-example-2",
                inputs: {
                    portA: ["Input A2"],
                    portB: ["Input B2"],
                },
            },
        ]);
    });

    it("should derive rule block input examples from linking black-box evaluation nodes", () => {
        const rule: ILinkingRule = ruleTestHelper.createLinkingRule({
            operator: ruleTestHelper.createComparisonOperator({
                id: "comparison-node",
                sourceInput: ruleTestHelper.createRuleBlockInput({
                    id: "rule-block-node",
                    ruleBlockId: "normalizeName",
                    bindings: [
                        {
                            portId: "portA",
                            input: ruleTestHelper.createPathInput({
                                id: "source-path-a",
                                path: "name",
                            }),
                        },
                    ],
                }),
                targetInput: ruleTestHelper.createPathInput({
                    id: "target-path",
                    path: "targetName",
                }),
            }) as IComparisonOperator,
        });
        const links: Pick<IEntityLink, "ruleValues">[] = [
            {
                ruleValues: {
                    operatorId: "comparison-node",
                    score: 1.0,
                    sourceValue: {
                        operatorId: "rule-block-node",
                        values: ["Rule block output"],
                        children: [
                            {
                                operatorId: "source-path-a",
                                values: ["Input A"],
                                children: [],
                            },
                        ],
                    },
                    targetValue: {
                        operatorId: "target-path",
                        values: ["Target"],
                        children: [],
                    },
                } as ComparisonConfidence,
            },
        ];

        expect(
            ruleBlockInternalEvaluationUtils.createInputExamplesFromLinkingEvaluations(
                links,
                "rule-block-node",
                rule.operator,
            ),
        ).toStrictEqual([
            {
                id: "inspection-example-1",
                inputs: {
                    portA: ["Input A"],
                },
            },
        ]);
    });

    it("should derive linking input examples for a target-side rule block nested below an aggregation", () => {
        const rule: ILinkingRule = ruleTestHelper.createLinkingRule({
            operator: ruleTestHelper.createAggregationOperator({
                id: "aggregation-node",
                inputs: [
                    ruleTestHelper.createComparisonOperator({
                        id: "comparison-node",
                        sourceInput: ruleTestHelper.createPathInput({
                            id: "source-path",
                            path: "name",
                        }),
                        targetInput: ruleTestHelper.createRuleBlockInput({
                            id: "rule-block-node",
                            ruleBlockId: "normalizeName",
                            bindings: [
                                {
                                    portId: "portA",
                                    input: ruleTestHelper.createPathInput({
                                        id: "target-path-a",
                                        path: "label",
                                    }),
                                },
                                {
                                    portId: "portB",
                                    input: ruleTestHelper.createPathInput({
                                        id: "target-path-b",
                                        path: "alias",
                                    }),
                                },
                            ],
                        }),
                    }) as IComparisonOperator,
                ],
            }),
        });
        const links: Pick<IEntityLink, "ruleValues">[] = [
            {
                ruleValues: {
                    operatorId: "aggregation-node",
                    children: [
                        {
                            operatorId: "comparison-node",
                            sourceValue: {
                                operatorId: "source-path",
                                values: ["Source"],
                                children: [],
                            },
                            targetValue: {
                                operatorId: "rule-block-node",
                                values: ["Rule block output"],
                                children: [
                                    {
                                        operatorId: "target-path-a",
                                        values: ["Input A"],
                                        children: [],
                                    },
                                    {
                                        operatorId: "target-path-b",
                                        values: ["Input B"],
                                        children: [],
                                    },
                                    {
                                        operatorId: "unbound-target",
                                        values: ["Ignored"],
                                        children: [],
                                    },
                                ],
                            },
                        } as ComparisonConfidence,
                    ],
                } as AggregationConfidence,
            },
        ];

        // This follows the less obvious traversal path: aggregation -> comparison -> target-side rule block. Only the
        // bound target children should become input port values, while unrelated target children are ignored.
        expect(
            ruleBlockInternalEvaluationUtils.createInputExamplesFromLinkingEvaluations(
                links,
                "rule-block-node",
                rule.operator,
            ),
        ).toStrictEqual([
            {
                id: "inspection-example-1",
                inputs: {
                    portA: ["Input A"],
                    portB: ["Input B"],
                },
            },
        ]);
    });
});
