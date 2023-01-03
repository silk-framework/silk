import React from "react";
import {FieldItem, TextArea, TextField} from "@eccenca/gui-elements";
import {errorMessage} from "./ParameterWidget";
import {useTranslation} from "react-i18next";
import {AdvancedOptionsArea} from "../../../AdvancedOptionsArea/AdvancedOptionsArea";
import CustomIdentifierInput, {handleCustomIdValidation} from "./CustomIdentifierInput";
import useErrorHandler from "../../../../../hooks/useErrorHandler";
import {Keyword} from "@ducks/workspace/typings";
import {SelectedParamsType} from "@eccenca/gui-elements/src/components/MultiSelect/MultiSelect";
import {MultiTagSelect} from "../../../MultiTagSelect";

interface IProps {
    form: any;
}

const LABEL = "label";
const DESCRIPTION = "description";
const IDENTIFIER = "id";
const TAGS = "tags";

/** The project create form */
export function ProjectForm({ form }: IProps) {
    const { register, errors, triggerValidation, setValue } = form;
    const [t] = useTranslation();
    const { registerError } = useErrorHandler();

    React.useEffect(() => {
        register({ name: LABEL }, { required: true });
        register({ name: DESCRIPTION });
        register({ name: IDENTIFIER });
        register({ name: TAGS });
    }, []);

    const onValueChange = (key) => {
        return async (e) => {
            const value = e.target ? e.target.value : e;
            setValue(key, value);
            await triggerValidation(key);
            //verify project identifier
            if (key === IDENTIFIER) handleCustomIdValidation(t, form, registerError, value);
        };
    };

    const handleTagSelectionChange = React.useCallback(
        (params: SelectedParamsType<Keyword>) => setValue("tags", params),
        []
    );

    return (
        <>
            <FieldItem
                key={LABEL}
                labelProps={{
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
                    hasStateDanger={errors.label ? true : false}
                    onChange={onValueChange(LABEL)}
                />
            </FieldItem>
            <FieldItem
                labelProps={{
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
            <FieldItem
                key={TAGS}
                labelProps={{
                    text: t("form.field.tags"),
                    htmlFor: TAGS,
                }}
            >
                <MultiTagSelect
                    handleTagSelectionChange={handleTagSelectionChange}
                />
            </FieldItem>
            <AdvancedOptionsArea>
                <CustomIdentifierInput form={form} onValueChange={onValueChange} />
            </AdvancedOptionsArea>
        </>
    );
}
