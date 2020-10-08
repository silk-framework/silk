import React, { useEffect } from "react";
import { FieldItem, TextField, TextArea } from "@gui-elements/index";
import { errorMessage } from "./ParameterWidget";
import { Intent } from "@gui-elements/blueprint/constants";
import { useTranslation } from "react-i18next";

export interface IProps {
    form: any;
    projectId: string;
}

const LABEL = "label";
const DESCRIPTION = "description";
/** The project create form */
export function ProjectForm({ form }: IProps) {
    const { register, errors, triggerValidation, setValue } = form;
    const [t] = useTranslation();

    useEffect(() => {
        register({ name: LABEL }, { required: true });
        register({ name: DESCRIPTION });
    }, [register]);
    const onValueChange = (key) => {
        return (e) => {
            const value = e.target ? e.target.value : e;
            setValue(key, value);
            triggerValidation();
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
        </>
    );
}
