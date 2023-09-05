import { RuleParameterInput } from "./RuleParameterInput";
import React from "react";
import { FieldItem } from "@eccenca/gui-elements";
import { IRuleNodeParameter } from "./RuleNodeParameter.typings";
import ruleNodeUtils from "./ruleNode.utils";
import { IParameterValidationResult } from "../../RuleEditor.typings";
import { useTranslation } from "react-i18next";
import { RuleEditorNodeParameterValue } from "../../model/RuleEditorModel.typings";
import { LanguageFilterProps } from "./PathInputOperator";

interface RuleNodeFormParameterProps {
    nodeId: string;
    pluginId: string;
    parameter: IRuleNodeParameter;
    /** Requests values of parameters this parameter might depend on for auto-completion. */
    dependentValue: (paramId: string) => string | undefined;
    /** If the form parameter will be rendered in a large area. The used input components might differ. */
    large: boolean;
    /** When used inside a modal, the behavior of some components will be optimized. */
    insideModal: boolean;
    /** If for this parameter there is a language filter supported. Currently only path parameters are affected by this option. */
    languageFilter?: LanguageFilterProps;
}

/** A single form parameter, i.e. label, validation and input component. */
export const RuleNodeFormParameter = ({
    nodeId,
    pluginId,
    parameter,
    dependentValue,
    large,
    insideModal,
    languageFilter,
}: RuleNodeFormParameterProps) => {
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

    console.log({ requiredLabel: paramSpec.requiredLabel });
    return (
        <FieldItem
            key={parameter.parameterId}
            labelProps={{
                text: paramSpec.label,
                tooltip: parameterDescription,
                tooltipProps: {
                    rootBoundary: "viewport",
                },
                info: paramSpec.requiredLabel || (paramSpec.required ? "required" : undefined),
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
                large={large}
                insideModal={insideModal}
                languageFilter={languageFilter}
            />
        </FieldItem>
    );
};
