import { Button } from "@eccenca/gui-elements";
import React, { useEffect } from "react";
import { useTranslation } from "react-i18next";

import { RuleEditorNodeParameterValue, ruleEditorNodeParameterValue } from "../../model/RuleEditorModel.typings";
import { RuleEditorBaseModal } from "../components/RuleEditorBaseModal";
import { RuleNodeParameterForm, RuleNodeParametersProps } from "./RuleNodeParameterForm";

type InheritedRuleNodeParameterProps = Omit<RuleNodeParametersProps, "large" | "insideModal">;

interface RuleNodeFormParameterModalProps extends InheritedRuleNodeParameterProps {
    onClose: () => any;
    /** Title of the modal */
    title: string;
    /** Updates several node parameters in a single transaction */
    updateNodeParameters: (nodeId: string, parameterValues: Map<string, RuleEditorNodeParameterValue>) => any;
}

/** Modal to edit rule node parameters. */
export const RuleNodeFormParameterModal = ({
    onClose,
    title,
    updateNodeParameters,
    ...ruleNodeParameterProps
}: RuleNodeFormParameterModalProps) => {
    const [t] = useTranslation();
    // The diff to the original values
    const [initialValues] = React.useState(new Map<string, RuleEditorNodeParameterValue | undefined>());
    const [parameterDiff] = React.useState(new Map<string, RuleEditorNodeParameterValue>());
    // Are there changes, yet
    const [dirty, setDirty] = React.useState(false);

    useEffect(() => {
        ruleNodeParameterProps.parameters.forEach((param) => {
            const initialValue = param.currentValue() ?? param.initialValue;
            initialValues.set(
                param.parameterId,
                initialValue && typeof initialValue === "object"
                    ? initialValue.value
                    : initialValue != null
                    ? initialValue
                    : undefined
            );
        });
    }, []);

    /** Do not propagate changes directly. Create a single change transaction when pressing Update. */
    const adaptedRuleNodeParameterProps: InheritedRuleNodeParameterProps = {
        ...ruleNodeParameterProps,
        parameters: ruleNodeParameterProps.parameters.map((param) => ({
            ...param,
            update: (newValue) => {
                const value = ruleEditorNodeParameterValue(newValue);
                // Set to dirty on first change
                if (parameterDiff.size === 0) {
                    setDirty(true);
                }
                if (value == null || ruleEditorNodeParameterValue(initialValues.get(param.parameterId)) === value) {
                    parameterDiff.delete(param.parameterId);
                } else {
                    parameterDiff.set(param.parameterId, newValue);
                }
                // If everything is changed back, unset dirty flag again
                if (parameterDiff.size === 0) {
                    setDirty(false);
                }
            },
        })),
    };

    const onUpdate = () => {
        updateNodeParameters(ruleNodeParameterProps.nodeId, parameterDiff);
        onClose();
    };

    return (
        <RuleEditorBaseModal
            data-test-id={"rule-node-parameter-form-modal"}
            size={"large"}
            hasBorder
            isOpen={true}
            title={title}
            preventSimpleClosing={true}
            canEscapeKeyClose={!dirty}
            canOutsideClickClose={!dirty}
            onClose={onClose}
            actions={[
                <Button key="update" affirmative={true} disabled={!dirty} onClick={onUpdate}>
                    {t("common.action.update")}
                </Button>,
                <Button key="cancel" onClick={onClose}>
                    {t("common.action.cancel")}
                </Button>,
            ]}
        >
            <RuleNodeParameterForm
                {...adaptedRuleNodeParameterProps}
                large={true}
                hasAdvancedSection={true}
                insideModal={true}
            />
        </RuleEditorBaseModal>
    );
};
