import React from "react";
import { Button, FieldItem, SimpleDialog, Switch, TextArea, TextField } from "@eccenca/gui-elements";
import { useTranslation } from "react-i18next";
import { RuleBlockPort } from "./ruleBlock.types";
import ruleBlockUtils from "./ruleBlock.utils";

export interface InputPortDialogSubmitValue {
    label: string;
    description: string;
    displayOrder: number;
    deprecated: boolean;
}

interface InputPortDialogProps {
    isOpen: boolean;
    mode: "create" | "edit";
    initialPort: InputPortDialogSubmitValue;
    existingPorts: RuleBlockPort[];
    persistedPorts: RuleBlockPort[];
    isRuleBlockInUse: boolean;
    editedPortId?: string;
    onClose: () => void;
    onSubmit: (value: InputPortDialogSubmitValue) => void;
}

/** Dialog for creating and updating logical rule block input ports. */
export const InputPortDialog = ({
    isOpen,
    mode,
    initialPort,
    existingPorts,
    persistedPorts,
    isRuleBlockInUse,
    editedPortId,
    onClose,
    onSubmit,
}: InputPortDialogProps) => {
    const [t] = useTranslation();
    const [label, setLabel] = React.useState(initialPort.label);
    const [description, setDescription] = React.useState(initialPort.description);
    const [displayOrder, setDisplayOrder] = React.useState(String(initialPort.displayOrder));
    const [deprecated, setDeprecated] = React.useState(initialPort.deprecated);

    React.useEffect(() => {
        setLabel(initialPort.label);
        setDescription(initialPort.description);
        setDisplayOrder(String(initialPort.displayOrder));
        setDeprecated(initialPort.deprecated);
    }, [
        editedPortId,
        initialPort.deprecated,
        initialPort.description,
        initialPort.displayOrder,
        initialPort.label,
        isOpen,
        mode,
    ]);

    const trimmedLabel = label.trim();
    const parsedDisplayOrder = Number.parseInt(displayOrder.trim(), 10);
    const displayOrderError = !displayOrder.trim()
        ? t("taskViews.ruleBlock.errors.displayOrderRequired")
        : !Number.isInteger(parsedDisplayOrder)
          ? t("taskViews.ruleBlock.errors.invalidDisplayOrder")
          : existingPorts.some(
                (port) => port.id !== editedPortId && port.displayOrder === parsedDisplayOrder,
            )
            ? t("taskViews.ruleBlock.errors.duplicateDisplayOrder", { displayOrder: parsedDisplayOrder })
            : undefined;
    const usedPortCompatibilityError =
        !displayOrderError && isRuleBlockInUse && editedPortId
            ? ruleBlockUtils.validateUsedPortUpdateCompatibility(
                  persistedPorts,
                  existingPorts,
                  editedPortId,
                  {
                      id: editedPortId,
                      label: trimmedLabel,
                      description,
                      displayOrder: parsedDisplayOrder,
                      deprecated,
                  },
                  (portLabel) => t("taskViews.ruleBlock.errors.usedPortReordered", { portLabel }),
              )
            : undefined;
    const labelError = !trimmedLabel
        ? t("taskViews.ruleBlock.errors.inputPortLabelRequired")
        : existingPorts.some((port) => port.id !== editedPortId && port.label.trim() === trimmedLabel)
          ? t("taskViews.ruleBlock.errors.duplicateInputPortLabel", { label: trimmedLabel })
          : undefined;

    const handleSubmit = () => {
        if (labelError || displayOrderError || usedPortCompatibilityError) {
            return;
        }
        onSubmit({
            label: trimmedLabel,
            description,
            displayOrder: parsedDisplayOrder,
            deprecated,
        });
    };

    return (
        <SimpleDialog
            isOpen={isOpen}
            size="small"
            title={
                mode === "create"
                    ? t("taskViews.ruleBlock.createInputPortTitle")
                    : t("taskViews.ruleBlock.editInputPortTitle")
            }
            onClose={onClose}
            actions={[
                <Button
                    key="submit"
                    affirmative
                    onClick={handleSubmit}
                    disabled={!!labelError || !!displayOrderError || !!usedPortCompatibilityError}
                    data-test-id="input-port-dialog-submit"
                >
                    {mode === "create" ? t("common.action.add") : t("common.action.update")}
                </Button>,
                <Button key="cancel" onClick={onClose}>
                    {t("common.action.cancel")}
                </Button>,
            ]}
        >
            <FieldItem
                labelProps={{ text: t("form.field.label"), htmlFor: "input-port-label" }}
                intent={labelError ? "danger" : undefined}
                messageText={labelError}
            >
                <TextField
                    id="input-port-label"
                    value={label}
                    onChange={(event: React.ChangeEvent<HTMLInputElement>) => setLabel(event.target.value)}
                    autoFocus
                />
            </FieldItem>
            <FieldItem
                labelProps={{ text: t("taskViews.ruleBlock.displayOrder"), htmlFor: "input-port-display-order" }}
                intent={displayOrderError || usedPortCompatibilityError ? "danger" : undefined}
                messageText={displayOrderError || usedPortCompatibilityError}
            >
                <TextField
                    id="input-port-display-order"
                    type="number"
                    value={displayOrder}
                    onChange={(event: React.ChangeEvent<HTMLInputElement>) => setDisplayOrder(event.target.value)}
                />
            </FieldItem>
            <FieldItem
                labelProps={{ text: t("common.words.description"), htmlFor: "input-port-description" }}
            >
                <TextArea
                    id="input-port-description"
                    value={description}
                    onChange={(event: React.ChangeEvent<HTMLTextAreaElement>) => setDescription(event.target.value)}
                    rows={3}
                    growVertically={false}
                />
            </FieldItem>
            <FieldItem labelProps={{ text: t("taskViews.ruleBlock.deprecated"), htmlFor: "input-port-deprecated" }}>
                <Switch
                    id="input-port-deprecated"
                    checked={deprecated}
                    onChange={(value: boolean) => setDeprecated(value)}
                />
            </FieldItem>
        </SimpleDialog>
    );
};

export default InputPortDialog;
