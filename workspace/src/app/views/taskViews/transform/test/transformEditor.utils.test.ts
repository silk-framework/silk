import transformEditorUtils from "../transformEditor.utils";
import { IRuleBlockOperatorDetails } from "../../ruleBlock/ruleBlockOperator.utils";
import { IRuleBlockSummary } from "../../ruleBlock/ruleBlock.types";

describe("transformEditorUtils", () => {
    it("should convert a rule block summary to a reusable operator summary with sorted ports", () => {
        const ruleBlockSummary: IRuleBlockSummary = {
            id: "normalizeName",
            label: "Normalize name",
            description: "Reusable normalization",
            ports: [
                {
                    id: "b",
                    label: "Second",
                    description: "",
                    displayOrder: 2,
                    deprecated: false,
                },
                {
                    id: "a",
                    label: "First",
                    description: "",
                    displayOrder: 1,
                    deprecated: false,
                },
            ],
        };

        const result = transformEditorUtils.ruleBlockSummaryToOperator(ruleBlockSummary);

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
                    displayOrder: 1,
                    deprecated: false,
                },
                {
                    id: "b",
                    label: "Second",
                    description: "",
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
                    displayOrder: 0,
                    deprecated: false,
                },
                {
                    id: "second",
                    label: "Second",
                    description: "",
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
