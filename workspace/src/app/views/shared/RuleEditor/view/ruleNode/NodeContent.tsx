import { IRuleNodeParameter } from "./RuleNodeParameter.typings";
import { RuleNodeParameterForm } from "./RuleNodeParameterForm";
import React from "react";
import { RuleOperatorNodeParameters } from "../../RuleEditor.typings";
import { IOperatorCreateContext, IOperatorNodeOperations } from "../../model/RuleEditorModel.utils";
import utils from "./ruleNode.utils";
import { IOperatorNodeParameterValueWithLabel } from "../../../../taskViews/shared/rules/rule.typings";
import { RuleNodeFormParameterModal } from "./RuleNodeFormParameterModal";
import { RuleEditorNodeParameterValue } from "../../model/RuleEditorModel.typings";
import { InputPathFunctions } from "./PathInputOperator";

export interface RuleNodeContentProps {
    nodeId: string;
    nodeLabel: string;
    tags?: string[];
    operatorContext: IOperatorCreateContext;
    nodeOperations: IOperatorNodeOperations;
    nodeParameters: RuleOperatorNodeParameters;
    /** Force an update of the content from the outside. */
    updateSwitch?: boolean;
    /** If the rule node form edit modal should be shown. */
    showEditModal: boolean;
    /** Must be called when the edit modal gets closed. */
    onCloseEditModal?: () => any;
}

/** The content of a rule node. */
export const NodeContent = ({
    nodeId,
    nodeLabel,
    tags,
    operatorContext,
    nodeOperations,
    nodeParameters,
    updateSwitch,
    showEditModal,
    onCloseEditModal = () => {},
}: RuleNodeContentProps) => {
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
                update: (value: RuleEditorNodeParameterValue) => {
                    nodeOperations.handleParameterChange(nodeId, paramId, value);
                },
                initialValue: initialValue ?? paramSpec.defaultValue,
                currentValue: () => operatorContext.currentValue(nodeId, paramId),
                parameterSpecification: paramSpec,
            };
        })
        // Sort by order given in the plugin spec
        .sort((paramA, paramB) => {
            return paramA.parameterSpecification.orderIdx < paramB.parameterSpecification.orderIdx ? -1 : 1;
        });
    const dependentValue = (paramId: string): string | undefined => {
        const value = operatorContext.currentValue(nodeId, paramId);
        if ((value as IOperatorNodeParameterValueWithLabel).value != null) {
            return (value as IOperatorNodeParameterValueWithLabel).value;
        } else {
            return value as string | undefined;
        }
    };
    const parameterDefaultValue: (paramId: string) => string | undefined = React.useCallback(
        (paramId) => {
            const spec = operatorContext.operatorParameterSpecification.get(paramId);
            if (spec) {
                return spec.defaultValue;
            } else {
                return undefined;
            }
        },
        [operatorContext.operatorParameterSpecification],
    );

    const inputPathFunctions: InputPathFunctions = React.useMemo(
        () => operatorContext.inputPathFunctions(nodeId),
        [operatorContext.inputPathFunctions, nodeId],
    );

    return rerender ? null : (
        <>
            {parameters.length ? (
                <RuleNodeParameterForm
                    key={"form"}
                    nodeId={nodeId}
                    pluginId={operatorContext.nodePluginId(nodeId) ?? "unknown"}
                    parameters={parameters}
                    dependentValue={dependentValue}
                    parameterDefaultValue={parameterDefaultValue}
                    large={false}
                    insideModal={false}
                    inputPathFunctions={inputPathFunctions}
                />
            ) : null}
            {tags ? utils.createOperatorTags(tags) : null}
            {showEditModal ? (
                <RuleNodeFormParameterModal
                    key={"formParameterModel"}
                    title={nodeLabel}
                    nodeId={nodeId}
                    pluginId={operatorContext.nodePluginId(nodeId) ?? "unknown"}
                    parameters={parameters}
                    dependentValue={dependentValue}
                    parameterDefaultValue={parameterDefaultValue}
                    onClose={() => {
                        setRerender(true);
                        onCloseEditModal();
                    }}
                    updateNodeParameters={operatorContext.updateNodeParameters}
                    inputPathFunctions={inputPathFunctions}
                />
            ) : null}
        </>
    );
};
