import { Button, SimpleDialog } from "gui-elements";
import React, { useEffect } from "react";
import { RuleNodeParameterForm, RuleNodeParametersProps } from "./RuleNodeParameterForm";
import { useTranslation } from "react-i18next";
import { RuleEditorUiContext } from "../../contexts/RuleEditorUiContext";

type InheritedRuleNodeParameterProps = Omit<RuleNodeParametersProps, "large">;

interface RuleNodeFormParameterModalProps extends InheritedRuleNodeParameterProps {
    onClose: () => any;
    /** Title of the modal */
    title: string;
    /** Updates several node parameters in a single transaction */
    updateNodeParameters: (nodeId: string, parameterValues: Map<string, string>) => any;
}

/** Modal to edit rule node parameters. */
export const RuleNodeFormParameterModal = ({
    onClose,
    title,
    updateNodeParameters,
    ...ruleNodeParameterProps
}: RuleNodeFormParameterModalProps) => {
    const [t] = useTranslation();
    const ruleEditorUiContext = React.useContext(RuleEditorUiContext);
    // The diff to the original values
    const [initialValues] = React.useState(new Map<string, string | undefined>());
    const [parameterDiff] = React.useState(new Map<string, string>());
    // Are there changes, yet
    const [dirty, setDirty] = React.useState(false);

    // Enable editor flag "modal shown" flag
    useEffect(() => {
        ruleEditorUiContext.setModalShown(true);
        return () => ruleEditorUiContext.setModalShown(false);
    }, []);

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
            update: (value) => {
                // Set to dirty on first change
                if (parameterDiff.size === 0) {
                    setDirty(true);
                }
                if (initialValues.get(param.parameterId) === value) {
                    parameterDiff.delete(param.parameterId);
                } else {
                    parameterDiff.set(param.parameterId, value);
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
        <SimpleDialog
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
            wrapperDivProps={{
                // Prevent react-flow from getting these events
                onContextMenu: (event) => event.stopPropagation(),
                onDrag: (event) => event.stopPropagation(),
                onDragStart: (event) => event.stopPropagation(),
                onDragEnd: (event) => event.stopPropagation(),
                onMouseDown: (event) => event.stopPropagation(),
                onMouseUp: (event) => event.stopPropagation(),
                onClick: (event) => event.stopPropagation(),
            }}
        >
            <RuleNodeParameterForm {...adaptedRuleNodeParameterProps} large={true} />
        </SimpleDialog>
    );
};
