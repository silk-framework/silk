import { Button, FieldItem, SimpleDialog, TextField } from "@eccenca/gui-elements";
import React from "react";
import { useTranslation } from "react-i18next";
import { Variable } from "./typing";
import { createNewVariable } from "./requests";
import TemplateValueInput from "../TemplateValueInput/TemplateValueInput";

interface VariableModalProps {
    variables: Variable[];
    projectId: string;
    taskId?: string;
    modalOpen: boolean;
    closeModal: () => void;
    targetVariable?: Variable;
    refresh: () => void;
}

export interface ValueStateRef {
    // The most recent value of the input component
    currentInputValue?: string;
    // The last input value before the switch happened from input -> template
    inputValueBeforeSwitch?: string;
    // The most recent template value
    currentTemplateValue: string;
    // The last template value before the switch happened from template -> input
    templateValueBeforeSwitch?: string;
}

const VariableModal: React.FC<VariableModalProps> = ({
    variables,
    projectId,
    taskId,
    modalOpen,
    closeModal,
    targetVariable,
    refresh,
}) => {
    const [loading, setLoading] = React.useState<boolean>(false);
    const [name, setName] = React.useState<string>("");
    const [description, setDescription] = React.useState<string>("");
    const [t] = useTranslation();

    const valueState = React.useRef<ValueStateRef>({
        // Input value needs to be undefined, so it gets set to the default value
        currentInputValue: targetVariable?.value,
        currentTemplateValue: targetVariable?.template ?? "",
    });

    React.useEffect(() => {
        setName(targetVariable?.name ?? "");
        setDescription(targetVariable?.description ?? "");
        valueState.current.inputValueBeforeSwitch = targetVariable?.value ?? "";
        valueState.current.templateValueBeforeSwitch = targetVariable?.template ?? "";
    }, [targetVariable]);

    const addNewVariable = React.useCallback(async () => {
        try {
            setLoading(true);
            const updatedVariables = {
                variables: [
                    ...variables,
                    {
                        name,
                        value: valueState.current.currentInputValue,
                        description,
                        template: valueState.current.currentTemplateValue,
                        isSensitive: false,
                        scope: taskId ? "task" : "project",
                    },
                ],
            };
            await createNewVariable(updatedVariables, projectId);
            refresh();
            closeModal();
        } catch (err) {
        } finally {
            setLoading(false);
        }
    }, [name, valueState, description, taskId]);

    return (
        <SimpleDialog
            data-test-id={"copy-item-to-modal"}
            size="small"
            title={`${targetVariable ? "Edit" : "Add"} Variable`}
            isOpen={modalOpen}
            onClose={closeModal}
            actions={[
                <Button
                    key="copy"
                    affirmative
                    onClick={addNewVariable}
                    disabled={loading || !(name.length && valueState.current.currentInputValue?.length)}
                    loading={loading}
                >
                    {t("widget.VariableWidget.actions.add", "Add")}
                </Button>,
                <Button key="cancel" onClick={closeModal}>
                    No, thanks
                </Button>,
            ]}
        >
            <FieldItem
                labelProps={{
                    htmlFor: "name",
                    text: "Name",
                }}
            >
                <TextField id="name" onChange={(e) => setName(e.target.value)} value={name} />
            </FieldItem>
            <TemplateValueInput ref={valueState} />
            <FieldItem
                labelProps={{
                    htmlFor: "description",
                    text: "Description",
                }}
            >
                <TextField id="description" onChange={(e) => setDescription(e.target.value)} value={description} />
            </FieldItem>
        </SimpleDialog>
    );
};

export default VariableModal;
