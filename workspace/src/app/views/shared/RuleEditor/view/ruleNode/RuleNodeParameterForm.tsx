import { IRuleNodeParameter } from "./RuleNodeParameter.typings";
import React from "react";
import { RuleNodeFormParameter } from "./RuleNodeFormParameter";

export interface RuleNodeParametersProps {
    nodeId: string;
    /** Plugin ID of the operator. */
    pluginId: string;
    parameters: IRuleNodeParameter[];
    /** Requests values of parameters this parameter might depend on for auto-completion. */
    dependentValue: (paramId: string) => string | undefined;
    /** If the form will be rendered in a large area. The used input components might differ. */
    large: boolean;
}

/** The parameter widget of a rule node. */
export const RuleNodeParameterForm = ({
    nodeId,
    pluginId,
    parameters,
    dependentValue,
    large,
}: RuleNodeParametersProps) => {
    return (
        <div key={"ruleNodeParameters"}>
            {parameters.map((param) => {
                return (
                    <RuleNodeFormParameter
                        key={param.parameterId}
                        nodeId={nodeId}
                        parameter={param}
                        dependentValue={dependentValue}
                        pluginId={pluginId}
                        large={large}
                    />
                );
            })}
        </div>
    );
};
