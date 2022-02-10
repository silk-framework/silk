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
                return (
                    <FieldItem
                        key={param.parameterId}
                        labelAttributes={{
                            text: param.parameterId, // TODO: real label
                            info: "TODO",
                        }}
                        // TODO: validation and error state
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
