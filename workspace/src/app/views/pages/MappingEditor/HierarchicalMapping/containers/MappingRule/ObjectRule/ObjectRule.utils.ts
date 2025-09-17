import { IComplexMappingRule, ITransformRule, PartialBy } from "../../../../../../taskViews/transform/transform.types";
import { IPathInput, ITransformOperator, IValueInput } from "../../../../../../taskViews/shared/rules/rule.typings";
import { MAPPING_ROOT_RULE_ID } from "../../../HierarchicalMapping";

/** The default URI pattern as it would be generated in the backend. */
export const defaultUriPattern = (containerRuleId: string) => {
    if (containerRuleId === MAPPING_ROOT_RULE_ID) {
        return "{}";
    } else {
        return `{}/${containerRuleId}`;
    }
};

export const defaultUriRule = (containerRuleId: string): PartialBy<IComplexMappingRule, "id" | "metadata"> => {
    const pathInput: IPathInput = {
        type: "pathInput",
        id: "path0",
        path: "",
    };
    const fixUriOp: ITransformOperator = {
        type: "transformInput",
        id: "fixUri0",
        function: "uriFix",
        inputs: [pathInput],
        parameters: {
            uriPrefix: "urn:url-encoded-value:",
        },
    };
    const inputs: (IPathInput | ITransformOperator)[] = [fixUriOp];
    if (containerRuleId !== MAPPING_ROOT_RULE_ID) {
        inputs.push({
            type: "transformInput",
            id: "constant1",
            function: "constant",
            inputs: [],
            parameters: {
                value: `/${containerRuleId}`,
            },
        });
    }
    const operator: ITransformOperator = {
        type: "transformInput",
        id: "buildUri",
        function: "concat",
        inputs,
        parameters: {
            glue: "",
            missingValuesAsEmptyStrings: "false",
        },
    };

    return {
        type: "complex",
        operator,
        sourcePaths: [],
        layout: {
            nodePositions: {},
        },
        uiAnnotations: {
            stickyNotes: [],
        },
    };
};
