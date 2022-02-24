import { IRuleNodeParameter } from "./RuleNodeParameter.typings";
import { RuleNodeParameterForm } from "./RuleNodeParameterForm";
import React from "react";
import { RuleOperatorNodeParameters } from "../../RuleEditor.typings";
import { IOperatorCreateContext, IOperatorNodeOperations } from "../../model/RuleEditorModel.utils";
import utils from "./ruleNode.utils";

interface NodeContentProps {
    nodeId: string;
    tags?: string[];
    operatorContext: IOperatorCreateContext;
    nodeOperations: IOperatorNodeOperations;
    nodeParameters: RuleOperatorNodeParameters;
    /** Force an update of the content from the outside. */
    updateSwitch?: boolean;
}

/** The content of a rule node. */
export const NodeContent = ({
    nodeId,
    tags,
    operatorContext,
    nodeOperations,
    nodeParameters,
    updateSwitch,
}: NodeContentProps) => {
    const [rerender, setRerender] = React.useState(false);

    /** Forced re-render logic. */
    React.useEffect(() => {
        if (typeof updateSwitch === "boolean") {
            setRerender(true);
        }
    }, [updateSwitch]);

    React.useEffect(() => {
        if (rerender) {
            setRerender(false);
        }
    }, [rerender]);

    const parameters: IRuleNodeParameter[] = Object.entries(nodeParameters)
        .filter(([paramId]) => operatorContext.operatorParameterSpecification.has(paramId))
        .map(([paramId, initialValue]) => {
            const paramSpec = operatorContext.operatorParameterSpecification.get(paramId)!!;
            return {
                parameterId: paramId,
                update: (value: string) => {
                    nodeOperations.handleParameterChange(nodeId, paramId, value);
                },
                initialValue: initialValue ?? paramSpec.defaultValue,
                currentValue: () => operatorContext.currentValue(nodeId, paramId),
                parameterSpecification: paramSpec,
            };
        });
    return rerender ? null : (
        <>
            {parameters.length ? <RuleNodeParameterForm key={"form"} nodeId={nodeId} parameters={parameters} /> : null}
            {tags ? utils.createOperatorTags(tags) : null}
        </>
    );
};
