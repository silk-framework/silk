import { IRuleNodeParameter } from "./RuleNodeParameter.typings";
import { RuleNodeParameterForm } from "./RuleNodeParameterForm";
import React from "react";
import { RuleOperatorNodeParameters } from "../../RuleEditor.typings";
import { IOperatorCreateContext, IOperatorNodeOperations } from "../../model/RuleEditorModel.utils";
import utils from "./ruleNode.utils";
import { IOperatorNodeParameterValueWithLabel } from "../../../../taskViews/shared/rules/rule.typings";

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
    const dependentValue = (paramId: string): string | undefined => {
        const value = operatorContext.currentValue(nodeId, paramId);
        if ((value as IOperatorNodeParameterValueWithLabel).value != null) {
            return (value as IOperatorNodeParameterValueWithLabel).value;
        } else {
            return value as string | undefined;
        }
    };
    return rerender ? null : (
        <>
            {parameters.length ? (
                <RuleNodeParameterForm
                    key={"form"}
                    nodeId={nodeId}
                    pluginId={operatorContext.nodePluginId(nodeId) ?? "unknown"}
                    parameters={parameters}
                    dependentValue={dependentValue}
                />
            ) : null}
            {tags ? utils.createOperatorTags(tags) : null}
        </>
    );
};
