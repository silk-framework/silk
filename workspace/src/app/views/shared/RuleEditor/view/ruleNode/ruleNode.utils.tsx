import { Highlighter, Spacing, Tag } from "@eccenca/gui-elements";
import React from "react";
import { IParameterSpecification, IParameterValidationResult } from "../../RuleEditor.typings";
import { ruleEditorNodeParameterValue, RuleEditorNodeParameterValue } from "../../model/RuleEditorModel.typings";
import Color from "color";

/** Adds highlighting to the text if query is non-empty. */
export const addHighlighting = (text: string, query?: string): string | React.JSX.Element => {
    return query ? <Highlighter label={text} searchValue={query} /> : text;
};

/** Creates the tags for an operator (node). */
const createOperatorTags = (
    tags: string[],
    query?: string,
    color?: (tag: string) => Color | string | undefined,
): React.JSX.Element[] => {
    const returnArray: React.JSX.Element[] = [];
    tags.forEach((tag, idx) => {
        returnArray.push(
            <Tag
                key={tag}
                minimal={true}
                small={true}
                backgroundColor={color ? color(tag) : undefined}
                emphasis={tag === "Recommended" ? "stronger" : "normal"}
            >
                {addHighlighting(tag, query)}
            </Tag>,
        );

        idx < tags.length + 1 && returnArray.push(<Spacing key={`spacing-${tag}`} vertical size="tiny" />);
    });
    return returnArray;
};

export const invalidValueResult = (message: string): IParameterValidationResult => ({
    valid: false,
    message,
    intent: "danger",
});
/** Validates a value of a specific parameter type. */
const validateValue = (
    parameterValue: RuleEditorNodeParameterValue,
    parameterSpec: IParameterSpecification,
    translate: (key: string, additionalParameters?: object) => string,
): IParameterValidationResult => {
    const value = ruleEditorNodeParameterValue(parameterValue);
    if (value == null) {
        if (parameterSpec.required) {
            return invalidValueResult(translate("form.validations.isRequired", { field: parameterSpec.label }));
        } else {
            return { valid: true };
        }
    }
    switch (parameterSpec.type) {
        case "int":
            const int = Number(value);
            return Number.isNaN(int) || !Number.isInteger(int)
                ? invalidValueResult(translate("form.validations.integer"))
                : { valid: true };
        case "float":
            const float = Number(value);
            return Number.isNaN(float) ? invalidValueResult(translate("form.validations.float")) : { valid: true };
        default:
            return {
                valid: true,
            };
    }
};

const ruleNodeUtils = {
    createOperatorTags,
    validateValue,
};

export default ruleNodeUtils;
