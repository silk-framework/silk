import React from "react";
import { FieldItem, TextField, TextArea } from "@gui-elements/index";
import { errorMessage, ParameterWidget } from "./ParameterWidget";
import { Intent } from "@gui-elements/blueprint/constants";
import { useTranslation } from "react-i18next";
import { AdvancedOptionsArea } from "../../../AdvancedOptionsArea/AdvancedOptionsArea";
import { requestProjectIdValidation } from "@ducks/common/requests";
import { debounce } from "../../../../../utils/debounce";

interface IProps {
    form: any;
}

const LABEL = "label";
const DESCRIPTION = "description";
const IDENTIFIER = "id";

/** The project create form */
export function ProjectForm({ form }: IProps) {
    const { register, errors, triggerValidation, setValue } = form;
    const [t] = useTranslation();

    /** check if custom task id is unique and is valid */
    const handleProjectIdValidation = React.useCallback(
        debounce(async (customProjectId?: string) => {
            try {
                if (customProjectId) {
                    const res = await requestProjectIdValidation(customProjectId);
                    if (res.axiosResponse.status === 200) {
                        form.clearError(IDENTIFIER);
                    }
                }
            } catch (err) {
                if (err.status === 409) {
                    form.setError("id", "pattern", "custom task id must be unique");
                } else {
                    triggerValidation(IDENTIFIER);
                }
            }
        }, 200),
        []
    );

    React.useEffect(() => {
        register({ name: LABEL }, { required: true });
        register({ name: DESCRIPTION });
        register(
            {
                name: IDENTIFIER,
            },
            {
                pattern: {
                    value: /^[a-zA-z0-9_-]*$/g,
                    message: t(
                        "form.validations.identifier",
                        "includes characters and numbers with only '_' & '-' as allowed special characters"
                    ),
                },
            }
        );
    }, [register]);
    const onValueChange = (key) => {
        return (e) => {
            const value = e.target ? e.target.value : e;
            setValue(key, value);
            //verify project identifier
            if (key === IDENTIFIER) {
                if (!value) form.clearError(IDENTIFIER);
                handleProjectIdValidation(value);
            } else {
                triggerValidation(key);
            }
        };
    };
    return (
        <>
            <FieldItem
                key={LABEL}
                labelAttributes={{
                    text: t("form.projectForm.title", "Title"),
                    info: t("common.words.required"),
                    htmlFor: "title-input",
                }}
                hasStateDanger={errorMessage("Title", errors.label) ? true : false}
                messageText={errorMessage("Title", errors.label)}
            >
                <TextField
                    id={LABEL}
                    placeholder={t("form.projectForm.projectTitle", "Project title")}
                    name={LABEL}
                    intent={errors.label ? Intent.DANGER : Intent.NONE}
                    onChange={onValueChange(LABEL)}
                />
            </FieldItem>
            <FieldItem
                labelAttributes={{
                    text: t("form.field.description"),
                    htmlFor: "desc-input",
                }}
            >
                <TextArea
                    id={DESCRIPTION}
                    name={DESCRIPTION}
                    growVertically={true}
                    placeholder={t("form.projectForm.projectDesc", "Project description")}
                    onChange={onValueChange(DESCRIPTION)}
                />
            </FieldItem>
            <AdvancedOptionsArea>
                <FieldItem
                    labelAttributes={{
                        text: "Project identifier",
                        htmlFor: IDENTIFIER,
                    }}
                    helperText={"A custom identifier e.g new-task-plugin"}
                    hasStateDanger={!!errorMessage(IDENTIFIER, errors.id)}
                    messageText={errorMessage(IDENTIFIER, errors.id)}
                >
                    <TextField
                        id={IDENTIFIER}
                        name={IDENTIFIER}
                        onChange={onValueChange(IDENTIFIER)}
                        intent={errors.id ? Intent.DANGER : Intent.NONE}
                        onKeyDown={(e) => {
                            if (e.keyCode === 13) {
                                e.preventDefault();
                                return false;
                            }
                        }}
                    />
                </FieldItem>
            </AdvancedOptionsArea>
        </>
    );
}
