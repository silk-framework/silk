import { Button, FieldItem, SimpleDialog, TextField } from "@eccenca/gui-elements";
import React from "react";
import { useTranslation } from "react-i18next";
import { Variable } from "./typing";
import { createNewVariable } from "./requests";

interface VariableModalProps {
    variables: Variable[];
    projectId: string;
    taskId?: string;
    modalOpen: boolean;
    closeModal: () => void;
    targetVariable?: Variable;
    refresh: () => void;
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
    const [value, setValue] = React.useState<string>("");
    const [description, setDescription] = React.useState<string>("");
    const [t] = useTranslation();

    React.useEffect(() => {
        setName(targetVariable?.name ?? "");
        setValue(targetVariable?.value ?? "");
        setDescription(targetVariable?.description ?? "");
    }, [targetVariable]);

    const addNewVariable = React.useCallback(async () => {
        try {
            setLoading(true);
            const updatedVariables = {
                variables: [
                    ...variables,
                    {
                        name,
                        value,
                        description,
                        template: "",
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
    }, [name, value, description, taskId]);

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
                    disabled={loading || !(name.length && value.length)}
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
            <FieldItem
                labelProps={{
                    htmlFor: "value",
                    text: "Value",
                }}
            >
                <TextField id="value" onChange={(e) => setValue(e.target.value)} value={value} />
            </FieldItem>
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
