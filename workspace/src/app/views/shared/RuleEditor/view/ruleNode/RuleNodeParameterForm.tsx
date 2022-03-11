import { IRuleNodeParameter } from "./RuleNodeParameter.typings";
import React from "react";
import { RuleNodeFormParameter } from "./RuleNodeFormParameter";

interface RuleNodeParametersProps {
    nodeId: string;
    parameters: IRuleNodeParameter[];
}

/** The parameter widget of a rule node. */
export const RuleNodeParameterForm = ({ nodeId, parameters }: RuleNodeParametersProps) => {
    return (
        <div key={"ruleNodeParameters"}>
            {parameters.map((param) => {
                return <RuleNodeFormParameter nodeId={nodeId} parameter={param} />;
            })}
        </div>
    );
};
