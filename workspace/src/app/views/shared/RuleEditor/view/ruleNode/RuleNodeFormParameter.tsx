import { RuleParameterInput } from "./RuleParameterInput";
import React from "react";
import { FieldItem } from "gui-elements";
import { IRuleNodeParameter } from "./RuleNodeParameter.typings";
import ruleNodeUtils from "./ruleNode.utils";
import { IParameterValidationResult } from "../../RuleEditor.typings";
import { useTranslation } from "react-i18next";

interface RuleNodeFormParameterProps {
    nodeId: string;
    parameter: IRuleNodeParameter;
}

/** A single form parameter, i.e. label, validation and input component. */
export const RuleNodeFormParameter = ({ nodeId, parameter }: RuleNodeFormParameterProps) => {
    const [t] = useTranslation();
    const [validationResult, setValidationResult] = React.useState<IParameterValidationResult>({ valid: true });
    const [validationState] = React.useState<{ timeoutId: number | undefined }>({ timeoutId: undefined });

    React.useEffect(() => {
        validate(parameter.currentValue());
    }, []);

    // Validate an updated value
    const validate = (value: string | undefined) => {
        const timeout = window.setTimeout(() => {
            if (parameter.parameterSpecification.customValidation) {
                setValidationResult(parameter.parameterSpecification.customValidation(value));
            } else {
                setValidationResult(ruleNodeUtils.validateValue(value, parameter.parameterSpecification, t));
            }
        }, 200);
        validationState.timeoutId != null && window.clearTimeout(validationState.timeoutId);
        validationState.timeoutId = timeout;
    };
    const updateWithValidation = (value: string) => {
        parameter.update(value);
        validate(value);
    };
    const paramSpec = parameter.parameterSpecification;
    const parameterDescription =
        paramSpec.description && paramSpec.description !== "No description" ? paramSpec.description : undefined;

    return (
        <FieldItem
            key={parameter.parameterId}
            labelAttributes={{
                text: paramSpec.label,
                tooltip: parameterDescription, // TODO: CMEM-3919 Tooltip flickers
                tooltipProperties: {
                    tooltipProps: {
                        placement: "top-right", // TODO: CMEM-3919: This does not work
                    },
                },
                info: paramSpec.required ? "required" : undefined,
            }}
            messageText={validationResult.message}
            hasStateDanger={validationResult.intent === "danger"}
            hasStatePrimary={validationResult.intent === "primary"}
            hasStateSuccess={validationResult.intent === "success"}
            hasStateWarning={validationResult.intent === "warning"}
        >
            <RuleParameterInput
                ruleParameter={{ ...parameter, update: updateWithValidation }}
                nodeId={nodeId}
                hasValidationError={!validationResult.valid}
            />
        </FieldItem>
    );
};
