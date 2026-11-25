import React from "react";
import { TextField } from "@eccenca/gui-elements";
import { errorMessage } from "./ParameterWidget";
import { requestProjectIdValidation, requestTaskIdValidation } from "@ducks/common/requests";
import useCopyButton from "../../../../../hooks/useCopyButton";
import { useTranslation } from "react-i18next";
import useErrorHandler from "../../../../../hooks/useErrorHandler";
import { FetchError } from "../../../../../services/fetch/responseInterceptor";
import { ArtefactFormParameter } from "./ArtefactFormParameter";
import { useFormState, type UseFormReturn } from "react-hook-form";

const IDENTIFIER = "id";

interface IProps {
    form: UseFormReturn;
    /** handles input change */
    onValueChange: (name: string) => React.ChangeEventHandler<HTMLInputElement>;

    /** existing task with preset id **/
    taskId?: string;

    /** existing project with preset id **/
    projectId?: string;
}

const CustomIdentifierInput = ({ form, onValueChange, taskId, projectId }: IProps) => {
    const { errors } = useFormState({ control: form.control, name: IDENTIFIER, exact: true });
    const [copyButton] = useCopyButton([{ text: taskId ?? "" }]);
    const [t] = useTranslation();
    const { registerError } = useErrorHandler();
    const validationVersion = React.useRef(0);
    const validationTimer = React.useRef<number>();
    const { control, getValues, subscribe } = form;

    React.useLayoutEffect(() => {
        const unsubscribe = subscribe({
            name: IDENTIFIER,
            exact: true,
            formState: { values: true },
            callback: ({ name }) => {
                if (!name) {
                    ++validationVersion.current;
                    window.clearTimeout(validationTimer.current);
                }
            },
        });
        return () => {
            unsubscribe();
            ++validationVersion.current;
            window.clearTimeout(validationTimer.current);
        };
    }, [control, subscribe, projectId, taskId]);

    const onChange: React.ChangeEventHandler<HTMLInputElement> = (event) => {
        const customId = event.target.value;
        const version = ++validationVersion.current;
        window.clearTimeout(validationTimer.current);
        onValueChange(IDENTIFIER)(event);
        const isCurrent = () => version === validationVersion.current && getValues(IDENTIFIER) === customId;

        validationTimer.current = window.setTimeout(async () => {
            if (!isCurrent()) return;
            if (!customId) {
                form.clearErrors(IDENTIFIER);
                return;
            }
            try {
                const response = projectId
                    ? await requestTaskIdValidation(customId, projectId)
                    : await requestProjectIdValidation(customId);
                if (isCurrent() && response.axiosResponse.status === 204) {
                    form.clearErrors(IDENTIFIER);
                }
            } catch (error) {
                if (!isCurrent()) return;
                const status = error instanceof FetchError ? error.httpStatus : undefined;
                if (status === 409 || status === 400) {
                    form.setError(IDENTIFIER, {
                        type: "manual",
                        message:
                            status === 409
                                ? t("CreateModal.CustomIdentifierInput.validations.unique")
                                : t("CreateModal.CustomIdentifierInput.validations.invalid"),
                    });
                } else {
                    registerError(
                        "CustomIdentifierInput.validate",
                        "There has been an error validating the custom ID.",
                        error,
                    );
                }
            }
        }, 200);
    };
    const otherProps = taskId ? { value: taskId } : {};

    return (
        <ArtefactFormParameter
            projectId={projectId}
            parameterId={IDENTIFIER}
            disabled={!!taskId}
            label={
                projectId
                    ? t("CreateModal.CustomIdentifierInput.TaskId")
                    : t("CreateModal.CustomIdentifierInput.ProjectId")
            }
            helperText={t("CreateModal.CustomIdentifierInput.helperDescription")}
            infoMessage={errorMessage(IDENTIFIER, errors.id)}
            inputElementFactory={() => (
                <TextField
                    id={IDENTIFIER}
                    name={IDENTIFIER}
                    onChange={onChange}
                    intent={errors.id ? "danger" : undefined}
                    onKeyDown={(e) => {
                        if (e.keyCode === 13) {
                            e.preventDefault();
                            return false;
                        }
                    }}
                    disabled={!!taskId}
                    rightElement={taskId ? copyButton : undefined}
                    {...otherProps}
                    escapeToBlur={true}
                />
            )}
        />
    );
};

export default CustomIdentifierInput;
