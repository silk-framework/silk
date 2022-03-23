import { RuleParameterInput } from "./RuleParameterInput";
import React from "react";
import { FieldItem } from "gui-elements";
import { IRuleNodeParameter } from "./RuleNodeParameter.typings";
import ruleNodeUtils from "./ruleNode.utils";
import { IParameterValidationResult } from "../../RuleEditor.typings";
import { useTranslation } from "react-i18next";
import { RuleEditorNodeParameterValue } from "../../model/RuleEditorModel.typings";
import { IOperatorNodeParameterValueWithLabel } from "../../../../taskViews/shared/rules/rule.typings";

interface RuleNodeFormParameterProps {
    nodeId: string;
    pluginId: string;
    parameter: IRuleNodeParameter;
    /** Requests values of parameters this parameter might depend on for auto-completion. */
    dependentValue: (paramId: string) => string | undefined;
}

/** A single form parameter, i.e. label, validation and input component. */
export const RuleNodeFormParameter = ({ nodeId, pluginId, parameter, dependentValue }: RuleNodeFormParameterProps) => {
    const [t] = useTranslation();
    const [validationResult, setValidationResult] = React.useState<IParameterValidationResult>({ valid: true });
    const [validationState] = React.useState<{ timeoutId: number | undefined }>({ timeoutId: undefined });

    React.useEffect(() => {
        validate(parameter.currentValue());
    }, []);

    // Validate an updated value
    const validate = (value: RuleEditorNodeParameterValue) => {
        const timeout = window.setTimeout(() => {
            let validationResult: IParameterValidationResult = { valid: true };
            if (parameter.parameterSpecification.customValidation) {
                validationResult = parameter.parameterSpecification.customValidation(value);
            } else {
                validationResult = ruleNodeUtils.validateValue(value, parameter.parameterSpecification, t);
            }
            if ((value as IOperatorNodeParameterValueWithLabel).label && validationResult.valid) {
                validationResult.message = (value as IOperatorNodeParameterValueWithLabel).label;
            }
            setValidationResult(validationResult);
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
                pluginId={pluginId}
                ruleParameter={{ ...parameter, update: updateWithValidation }}
                nodeId={nodeId}
                hasValidationError={!validationResult.valid}
                dependentValue={dependentValue}
            />
        </FieldItem>
    );
};
