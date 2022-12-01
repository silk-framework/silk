import { requestProjectIdValidation, requestTaskIdValidation } from "@ducks/common/requests";
import { FieldItem, TextField } from "@eccenca/gui-elements";
import { TFunction } from "i18next";
import React from "react";
import { useTranslation } from "react-i18next";

import useCopyButton from "../../../../../hooks/useCopyButton";
import { ErrorHandlerRegisterFuncType } from "../../../../../hooks/useErrorHandler";
import { debounce } from "../../../../../utils/debounce";
import { errorMessage } from "./ParameterWidget";

const IDENTIFIER = "id";

interface IProps {
    form: any;
    /** handles input change */
    onValueChange: (val: string) => (event: any) => Promise<void>;

    /** existing task with preset id **/
    taskId?: string;

    /** existing project with preset id **/
    projectId?: string;
}

/**
 * validate the custom ids checking for uniqueness and sanity
 * @param form
 * @param projectId
 * @returns
 */
export const handleCustomIdValidation = debounce(
    async (
        t: TFunction,
        form: any,
        registerError: ErrorHandlerRegisterFuncType,
        customId: string,
        projectId?: string
    ) => {
        if (!customId) return form.clearError(IDENTIFIER);
        try {
            const res = !projectId
                ? await requestProjectIdValidation(customId)
                : await requestTaskIdValidation(customId, projectId);
            if (res.axiosResponse.status === 204) {
                form.clearError(IDENTIFIER);
            }
        } catch (err) {
            if (err.httpStatus === 409) {
                form.setError("id", "manual", t("CreateModal.CustomIdentifierInput.validations.unique"));
            } else if (err.httpStatus === 400) {
                form.setError("id", "manual", t("CreateModal.CustomIdentifierInput.validations.invalid"));
            } else {
                registerError("handleCustomIdValidation", "There has been an error validating the custom ID.", err);
            }
        }
    },
    200
);

const CustomIdentifierInput = ({ form, onValueChange, taskId, projectId }: IProps) => {
    const { errors } = form;
    const [copyButton] = useCopyButton([{ text: taskId ?? "" }]);
    const [t] = useTranslation();
    const otherProps = taskId ? { value: taskId } : {};

    return (
        <FieldItem
            disabled={!!taskId}
            labelProps={{
                text: projectId
                    ? t("CreateModal.CustomIdentifierInput.TaskId")
                    : t("CreateModal.CustomIdentifierInput.ProjectId"),
                htmlFor: IDENTIFIER,
            }}
            helperText={t("CreateModal.CustomIdentifierInput.helperDescription")}
            hasStateDanger={!!errorMessage(IDENTIFIER, errors.id)}
            messageText={errorMessage(IDENTIFIER, errors.id)}
        >
            <TextField
                id={IDENTIFIER}
                name={IDENTIFIER}
                onChange={onValueChange(IDENTIFIER)}
                hasStateDanger={errors.id ? true : false}
                onKeyDown={(e) => {
                    if (e.keyCode === 13) {
                        e.preventDefault();
                        return false;
                    }
                }}
                disabled={!!taskId}
                rightElement={taskId ? copyButton : undefined}
                {...otherProps}
            />
        </FieldItem>
    );
};

export default CustomIdentifierInput;
