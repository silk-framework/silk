import React from "react";
import { CodeEditor, FieldItem, TextField, MultiSuggestFieldSelectionProps } from "@eccenca/gui-elements";
import { errorMessage } from "./ParameterWidget";
import { useTranslation } from "react-i18next";
import { AdvancedOptionsArea } from "../../../AdvancedOptionsArea/AdvancedOptionsArea";
import CustomIdentifierInput from "./CustomIdentifierInput";
import { Keyword } from "@ducks/workspace/typings";
import { MultiTagSelect } from "../../../MultiTagSelect";
import useHotKey from "../../../HotKeyHandler/HotKeyHandler";
import { AccessControlConfig } from "@ducks/workspace/requests";
import { useProjectAclManagementComponent } from "../../../../../hooks/useProjectAclManagementComponent";
import { useFormState, type Control, type FieldValues, type UseFormReturn } from "react-hook-form";

interface IProps {
    form: UseFormReturn;

    /** Called when no changes were done in the form and the ESC key is pressed. */
    goBackOnEscape?: () => any;
    updateProjectAcl: (projectAcl: AccessControlConfig) => void;
}

const LABEL = "label";
const DESCRIPTION = "description";
const IDENTIFIER = "id";
const TAGS = "tags";

function ProjectTitleField({
    control,
    onChange,
}: {
    control: Control<FieldValues>;
    onChange: React.ChangeEventHandler<HTMLInputElement>;
}) {
    const [t] = useTranslation();
    const { errors } = useFormState({ control, name: LABEL, exact: true });
    const title = t("form.projectForm.title");
    return (
        <FieldItem
            labelProps={{
                text: title,
                info: t("common.words.required"),
                htmlFor: LABEL,
            }}
            intent={errors.label ? "danger" : undefined}
            messageText={errorMessage(title, errors.label)}
        >
            <TextField
                id={LABEL}
                placeholder={t("form.projectForm.projectTitle")}
                name={LABEL}
                autoFocus
                intent={errors.label ? "danger" : undefined}
                onChange={onChange}
                escapeToBlur
            />
        </FieldItem>
    );
}

/** The project create form */
export function ProjectForm({ form, goBackOnEscape = () => {}, updateProjectAcl }: IProps) {
    const { register, trigger, setValue } = form;
    const [t] = useTranslation();
    const escapeKeyDisabled = React.useRef(false);
    const aclManagement = useProjectAclManagementComponent({
        onChange: updateProjectAcl,
        externalInitialAclGroups: { groups: [] },
    });

    const handleEscapeKey = React.useCallback(() => {
        if (!escapeKeyDisabled.current) {
            goBackOnEscape();
        }
    }, []);

    useHotKey({ hotkey: "escape", handler: handleEscapeKey });

    React.useEffect(() => {
        register(LABEL, { required: true });
        register(DESCRIPTION);
        register(IDENTIFIER);
        register(TAGS);
    }, []);

    React.useEffect(() => {
        if (aclManagement.enabled) {
            updateProjectAcl({ groups: [] });
        }
    }, [aclManagement.enabled]);

    const onValueChange = (key) => {
        return async (e) => {
            const value = e.target ? e.target.value : e;
            setValue(key, value);
            await trigger(key);
            if (!escapeKeyDisabled.current) {
                escapeKeyDisabled.current = true;
            }
        };
    };

    const handleTagSelectionChange = React.useCallback(
        (params: MultiSuggestFieldSelectionProps<Keyword>) => {
            setValue(TAGS, params);
            if ((params.newlySelected || params.newlyRemoved) && !escapeKeyDisabled.current) {
                escapeKeyDisabled.current = true;
            }
        },
        [setValue],
    );

    const CodeEditorMemoed = React.useMemo(
        () => (
            <CodeEditor
                mode="markdown"
                useToolbar
                id={DESCRIPTION}
                name={DESCRIPTION}
                onChange={onValueChange(DESCRIPTION)}
                data-test-id="codemirror-wrapper"
                translate={(key) => {
                    const translationKey = `Editor.markdown.toolbar.${key}`;
                    return t(translationKey) as string;
                }}
            />
        ),
        [],
    );

    return (
        <>
            <ProjectTitleField control={form.control} onChange={onValueChange(LABEL)} />
            <FieldItem
                labelProps={{
                    text: t("form.field.description"),
                    htmlFor: "desc-input",
                }}
                helperText={
                    <p>
                        {t("Metadata.markdownHelperText")}{" "}
                        <a href="https://www.markdownguide.org/cheat-sheet" target="_blank">
                            {t("Metadata.markdownHelperLinkText")}
                        </a>
                        .
                    </p>
                }
            >
                {CodeEditorMemoed}
            </FieldItem>
            <FieldItem
                key={TAGS}
                labelProps={{
                    text: t("form.field.tags"),
                    htmlFor: TAGS,
                }}
            >
                <MultiTagSelect handleTagSelectionChange={handleTagSelectionChange} />
            </FieldItem>
            {aclManagement.component}
            <AdvancedOptionsArea>
                <CustomIdentifierInput form={form} onValueChange={onValueChange} />
            </AdvancedOptionsArea>
        </>
    );
}
