import transformEditorUtils, { IRuleBlockOperatorDetails } from "./transformEditor.utils";
import { IProjectTask } from "@ducks/shared/typings";
import { IRuleBlockTaskParameters } from "../ruleBlock/ruleBlock.types";

describe("transformEditorUtils", () => {
    it("should convert a rule block task to a reusable operator summary with sorted ports", () => {
        const ruleBlockTask: IProjectTask<IRuleBlockTaskParameters> = {
            id: "normalizeName",
            project: "projectA",
            taskType: "RuleBlock",
            metadata: {
                label: "Normalize name",
                description: "Reusable normalization",
            },
            data: {
                type: "ruleBlock",
                parameters: {
                    ruleBlockModel: {
                        ports: [
                            {
                                id: "b",
                                label: "Second",
                                description: "",
                                exampleValues: "",
                                displayOrder: 2,
                                deprecated: false,
                            },
                            {
                                id: "a",
                                label: "First",
                                description: "",
                                exampleValues: "",
                                displayOrder: 1,
                                deprecated: false,
                            },
                        ],
                        layout: { nodePositions: {} },
                    },
                },
                taskType: "RuleBlock",
            },
        };

        const result = transformEditorUtils.ruleBlockTaskToOperator(ruleBlockTask);

        expect(result).toStrictEqual({
            pluginType: "RuleBlock",
            pluginId: "normalizeName",
            label: "Normalize name",
            description: "Reusable normalization",
            ports: [
                {
                    id: "a",
                    label: "First",
                    description: "",
                    exampleValues: "",
                    displayOrder: 1,
                    deprecated: false,
                },
                {
                    id: "b",
                    label: "Second",
                    description: "",
                    exampleValues: "",
                    displayOrder: 2,
                    deprecated: false,
                },
            ],
        });
    });

    it("should convert a reusable rule block summary to a named-port rule operator", () => {
        const operatorSummary: IRuleBlockOperatorDetails = {
            pluginType: "RuleBlock",
            pluginId: "normalizeName",
            label: "Normalize name",
            description: "Reusable normalization",
            ports: [
                {
                    id: "first",
                    label: "First",
                    description: "",
                    exampleValues: "",
                    displayOrder: 0,
                    deprecated: false,
                },
                {
                    id: "second",
                    label: "Second",
                    description: "",
                    exampleValues: "",
                    displayOrder: 1,
                    deprecated: false,
                },
            ],
        };

        const result = transformEditorUtils.convertRuleBlockOperator(operatorSummary);

        expect(result).toMatchObject({
            pluginType: "RuleBlock",
            pluginId: "normalizeName",
            label: "Normalize name",
            description: "Reusable normalization",
            icon: "artefact-ruleblock",
            inputsCanBeSwitched: false,
            parameterSpecification: {},
        });
        expect(result.portSpecification).toStrictEqual({
            type: "named",
            inputPorts: [{ id: "first" }, { id: "second" }],
        });
    });
});
