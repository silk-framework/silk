import { Button, FieldItem, Notification, SimpleDialog, TextArea, TextField } from "@eccenca/gui-elements";
import React from "react";
import { useTranslation } from "react-i18next";
import { Variable } from "../typing";
import { createNewVariable } from "../requests";
import TemplateValueInput from "../../../../views/shared/TemplateValueInput/TemplateValueInput";
import { current } from "@reduxjs/toolkit";

interface VariableModalProps {
    /*
       All the existing variables for the project,
       this is tenable because first iteration expects a max of 7 variables 
     */
    variables: Variable[];
    /**
     * id for the project, variables are per project.
     */
    projectId: string;
    /**
     * id for the project's task
     */
    taskId?: string;
    /**
     * delimiter that determines if the modal is shown or not
     */
    modalOpen: boolean;
    /**
     * control handler that closes the modal
     */
    closeModal: () => void;
    /**
     * in the case of editing an existing variable,
     * this is the variable in particular that is getting edited.
     * for creating a new variable, this is undefined.
     */
    targetVariable?: Variable;
    /**
     * control trigger that notifies parent component to refetch all the variables,
     * usually after creating a new variable or editing an existing one
     */
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

const NewVariableModal: React.FC<VariableModalProps> = ({
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
    const [validationError, setValidationError] = React.useState<
        Partial<{ name: string; valueOrTemplate: string }> | undefined
    >();
    const [errorMessage, setErrorMessage] = React.useState<string>("");
    const [t] = useTranslation();
    const isEditMode = targetVariable;

    const valueState = React.useRef<ValueStateRef>({
        // Input value needs to be undefined, so it gets set to the default value
        currentInputValue: targetVariable?.value ?? "",
        currentTemplateValue: targetVariable?.template ?? "",
    });

    React.useEffect(() => {
        setName(targetVariable?.name ?? "");
        setDescription(targetVariable?.description ?? "");
        valueState.current = {
            inputValueBeforeSwitch: targetVariable?.value ?? "",
            templateValueBeforeSwitch: targetVariable?.template ?? "",
            currentTemplateValue: targetVariable?.template ?? "",
            currentInputValue: targetVariable?.value ?? "",
        };
        setErrorMessage("");
    }, [targetVariable]);

    /**
     * checks that the value or template input fields are not empty
     */
    const validationChecker = React.useCallback(() => {
        const error = {
            name: !name.length ? t("widget.VariableWidget.formErrors.nameMustBeSpecified") : undefined,
            valueOrTemplate:
                !valueState.current.currentInputValue?.length && !valueState.current.currentTemplateValue?.length
                    ? t("widget.VariableWidget.formErrors.mustProvideValOrTemplate")
                    : undefined,
        };
        setValidationError(error);
        return error;
    }, [name, valueState]);

    /**
     * handles name input changes and ensures no duplicated variable names
     */
    const handleVariableNameChange = React.useCallback(
        (e) => {
            const newName = e.target.value;
            setName(newName);
            if (variables.find((v) => v.name === newName)) {
                setValidationError((prevError) => ({
                    ...prevError,
                    name: t("widget.VariableWidget.formErrors.nameAlreadyExists"),
                }));
            } else {
                setValidationError((prevError) => ({ ...prevError, name: undefined }));
            }
        },
        [variables]
    );

    const resetModalState = React.useCallback(() => {
        setName("");
        setDescription("");
        valueState.current.inputValueBeforeSwitch = "";
        valueState.current.templateValueBeforeSwitch = "";
    }, []);

    /**
     * adds new variable or updates existing variable
     */
    const addNewVariable = React.useCallback(async () => {
        const error = validationChecker();
        if (error?.name || error?.valueOrTemplate) return;
        try {
            setLoading(true);
            setErrorMessage("");
            const newVar = {
                name,
                value: valueState.current.currentInputValue || null,
                description,
                template: valueState.current.currentTemplateValue || null,
                isSensitive: false,
                scope: taskId ? "task" : "project",
            } as const;

            const { variableAlreadyExist, newVariables } = variables.reduce(
                (acc, variable) => {
                    if (variable.name === name) {
                        acc.newVariables.push({
                            ...variable,
                            ...newVar,
                        });
                        acc.variableAlreadyExist = true;
                    } else {
                        acc.newVariables.push(variable);
                    }
                    return acc;
                },
                { newVariables: [] as Variable[], variableAlreadyExist: false }
            );

            if (!variableAlreadyExist) {
                newVariables.push(newVar);
            }

            const updatedVariables = {
                variables: newVariables,
            };
            await createNewVariable(updatedVariables, projectId, taskId);
            resetModalState();
            refresh();
            closeModal();
        } catch (err) {
            setErrorMessage(err?.body?.detail);
        } finally {
            setLoading(false);
        }
    }, [name, valueState, description, taskId]);

    const handleModalClose = React.useCallback(() => {
        closeModal();
        resetModalState();
    }, []);

    return (
        <SimpleDialog
            data-test-id="variable-modal"
            size="small"
            title={`${isEditMode ? "Edit" : "Add"} Variable`}
            isOpen={modalOpen}
            onClose={handleModalClose}
            notifications={errorMessage ? <Notification danger>{errorMessage}</Notification> : null}
            actions={[
                <Button
                    key="add"
                    data-test-id="variable-modal-submit-btn"
                    affirmative
                    onClick={addNewVariable}
                    disabled={loading}
                    loading={loading}
                >
                    {!isEditMode ? t("common.action.add") : t("common.action.update")}
                </Button>,
                <Button key="cancel" onClick={handleModalClose}>
                    {t("common.action.cancel", "Cancel")}
                </Button>,
            ]}
        >
            <FieldItem
                hasStateDanger={!!validationError?.name}
                messageText={validationError?.name}
                labelProps={{
                    htmlFor: "name",
                    text: t("widget.VariableWidget.form.name", "Name"),
                }}
            >
                <TextField
                    id="name"
                    intent={!!validationError?.name ? "danger" : "none"}
                    onChange={handleVariableNameChange}
                    value={name}
                    disabled={!!isEditMode}
                />
            </FieldItem>
            <TemplateValueInput
                messageText={validationError?.valueOrTemplate}
                hasStateDanger={!!validationError?.valueOrTemplate}
                ref={valueState}
                projectId={projectId}
            />
            <FieldItem
                labelProps={{
                    htmlFor: "description",
                    text: t("widget.VariableWidget.form.description", "Description"),
                }}
            >
                <TextArea id="description" onChange={(e) => setDescription(e.target.value)} value={description} />
            </FieldItem>
        </SimpleDialog>
    );
};

export default NewVariableModal;
