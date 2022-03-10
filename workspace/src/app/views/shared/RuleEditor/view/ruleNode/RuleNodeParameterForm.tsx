import { IRuleNodeParameter } from "./RuleNodeParameter.typings";
import { RuleParameterInput } from "./RuleParameterInput";
import React from "react";
import { FieldItem } from "gui-elements";

interface RuleNodeParametersProps {
    nodeId: string;
    parameters: IRuleNodeParameter[];
}

/** The parameter widget of a rule node. */
export const RuleNodeParameterForm = ({ nodeId, parameters }: RuleNodeParametersProps) => {
    return (
        <div key={"ruleNodeParameters"}>
            {parameters.map((param) => {
                const paramSpec = param.parameterSpecification;
                const parameterDescription =
                    paramSpec.description && paramSpec.description !== "No description"
                        ? paramSpec.description
                        : undefined;
                return (
                    <FieldItem
                        key={param.parameterId}
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
                        // TODO: CMEM-3919 validation and error state
                        messageText={"La la"}
                        // hasStateDanger={!!errorMessage("Label", errors.label)}
                        // messageText={errorMessage("Label", errors.label)}
                    >
                        <RuleParameterInput key={param.parameterId} {...param} nodeId={nodeId} />
                    </FieldItem>
                );
            })}
        </div>
    );
};
