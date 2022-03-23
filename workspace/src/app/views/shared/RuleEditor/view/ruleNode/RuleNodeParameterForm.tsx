import { IRuleNodeParameter } from "./RuleNodeParameter.typings";
import React from "react";
import { RuleNodeFormParameter } from "./RuleNodeFormParameter";

interface RuleNodeParametersProps {
    nodeId: string;
    /** Plugin ID of the operator. */
    pluginId: string;
    parameters: IRuleNodeParameter[];
    /** Requests values of parameters this parameter might depend on for auto-completion. */
    dependentValue: (paramId: string) => string | undefined;
}

/** The parameter widget of a rule node. */
export const RuleNodeParameterForm = ({ nodeId, pluginId, parameters, dependentValue }: RuleNodeParametersProps) => {
    return (
        <div key={"ruleNodeParameters"}>
            {parameters.map((param) => {
                return (
                    <RuleNodeFormParameter
                        nodeId={nodeId}
                        parameter={param}
                        dependentValue={dependentValue}
                        pluginId={pluginId}
                    />
                );
            })}
        </div>
    );
};
