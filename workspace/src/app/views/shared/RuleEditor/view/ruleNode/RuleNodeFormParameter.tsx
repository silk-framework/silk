import { RuleParameterInput } from "./RuleParameterInput";
import React from "react";
import { FieldItem, Link, Icon } from "@eccenca/gui-elements";
import { IRuleNodeParameter } from "./RuleNodeParameter.typings";
import ruleNodeUtils from "./ruleNode.utils";
import { IParameterValidationResult } from "../../RuleEditor.typings";
import { useTranslation } from "react-i18next";
import { RuleEditorNodeParameterValue } from "../../model/RuleEditorModel.typings";
import { InputPathFunctions } from "./PathInputOperator";

interface RuleNodeFormParameterProps {
    nodeId: string;
    pluginId: string;
    parameter: IRuleNodeParameter;
    /** Requests values of parameters this parameter might depend on for auto-completion. */
    dependentValue: (paramId: string) => string | undefined;
    /** The default value as defined in the parameter spec. */
    parameterDefaultValue: (paramId: string) => string | undefined;
    /** If the form parameter will be rendered in a large area. The used input components might differ. */
    large: boolean;
    /** When used inside a modal, the behavior of some components will be optimized. */
    insideModal: boolean;
    /** Functions that are specific to input path rule operators. */
    inputPathFunctions: InputPathFunctions;
}

/** A single form parameter, i.e. label, validation and input component. */
export const RuleNodeFormParameter = ({
    nodeId,
    pluginId,
    parameter,
    dependentValue,
    large,
    insideModal,
    inputPathFunctions,
    parameterDefaultValue,
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
                additionalElements: paramSpec.urlUserHelp ? (
                    <Link href={paramSpec.urlUserHelp} target="_docs" style={{ color: "inherit" }}>
                        <Icon small name="item-question" tooltipText={t("common.documentationLink.tooltip")} />
                    </Link>
                ) : undefined,
            }}
            messageText={validationResult.message}
            intent={validationResult.intent}
        >
            <RuleParameterInput
                pluginId={pluginId}
                ruleParameter={{ ...parameter, update: updateWithValidation }}
                nodeId={nodeId}
                hasValidationError={!validationResult.valid}
                dependentValue={dependentValue}
                parameterDefaultValue={parameterDefaultValue}
                large={large}
                insideModal={insideModal}
                inputPathFunctions={inputPathFunctions}
            />
        </FieldItem>
    );
};
