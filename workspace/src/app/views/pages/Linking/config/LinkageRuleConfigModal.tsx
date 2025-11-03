import React, { useEffect, useState } from "react";
import { Button, FieldItem, IconButton, Notification, SimpleDialog, TextField, Switch } from "@eccenca/gui-elements";
import { useTranslation } from "react-i18next";
import { LinkageRuleConfigItem } from "./LinkageRuleConfig";
import { IAutocompleteDefaultResponse } from "@ducks/shared/typings";
import { DefaultAutoCompleteField } from "../../../shared/autoCompletion/DefaultAutoCompleteField";
import { FetchResponse } from "../../../../services/fetch/responseInterceptor";
import useErrorHandler from "../../../../hooks/useErrorHandler";

interface IProps {
    onClose: () => any;
    parameters: LinkageRuleConfigItem[];
    submit: (parameters: [string, string | boolean | undefined][]) => any;
}

/** Config modal to change linkage rule config parameters like link type and link limit. */
export const LinkageRuleConfigModal = ({ onClose, parameters, submit }: IProps) => {
    const [t] = useTranslation();
    const [parameterDiff] = useState<Map<string, string>>(new Map());
    const [changed, setChanged] = useState(false);
    const [requestError, setRequestError] = useState<JSX.Element | undefined>(undefined);
    const [errorCount, setErrorCount] = useState(0);
    const initialParameters = new Map(parameters.map((p) => [p.id, p]));
    const [errors] = useState(new Map<string, string>());
    const [saving, setSaving] = useState(false);
    const { registerError } = useErrorHandler();

    // Always change error count to correct count when it is off. This mechanism is sometimes used to re-render.
    useEffect(() => {
        if (errorCount !== errors.size) {
            setErrorCount(errors.size);
        }
    }, [errorCount]);

    const changeParameterFromEvent = (parameterId: string) => {
        const changeParameterValue = changeParameter(parameterId);
        return (event: React.ChangeEvent<HTMLInputElement>) => {
            const value = event.target.value;
            changeParameterValue(value);
        };
    };

    const wrapAutoCompleteRequest =
        (
            forParameter: string,
            autoCompleteRequest: (
                textQuery: string,
                limit: number,
            ) => Promise<FetchResponse<IAutocompleteDefaultResponse[]>>,
        ) =>
        async (textQuery: string) => {
            try {
                return (await autoCompleteRequest(textQuery, 50)).data;
            } catch (err) {
                setRequestError(
                    <Notification
                        actions={<IconButton onClick={() => setRequestError(undefined)} name={"navigation-close"} />}
                        intent="warning"
                    >
                        Auto-completion request has failed. Cannot suggest values for '${forParameter}' parameter.
                    </Notification>,
                );
                return [];
            }
        };

    const changeParameter = (parameterId: string) => (value: string) => {
        const hasChanges = parameterDiff.size > 0;
        const validation = initialParameters.get(parameterId)?.validation(value);
        // Validation
        if (typeof validation === "string") {
            const newError = !errors.has(parameterId);
            const currentError = errors.get(parameterId);
            errors.set(parameterId, validation);
            if (newError) {
                setErrorCount(errors.size);
            } else {
                if (currentError !== validation) {
                    setErrorCount(errors.size + 1);
                }
            }
        } else {
            const errorResolved = errors.has(parameterId);
            if (errorResolved) {
                errors.delete(parameterId);
                setErrorCount(errors.size);
            }
        }
        // Add to parameter diff
        if (initialParameters.get(parameterId)?.value === value) {
            parameterDiff.delete(parameterId);
        } else {
            parameterDiff.set(parameterId, value);
        }
        if (parameterDiff.size && !hasChanges) {
            setChanged(true);
        } else if (parameterDiff.size === 0 && hasChanges) {
            setChanged(false);
        }
    };

    const onSubmit = async () => {
        const updatedParameters: [string, string | boolean | undefined][] = parameters.map((p) => {
            const updatedValue = parameterDiff.get(p.id) ?? p.value;
            return [p.id, updatedValue];
        });
        setSaving(true);
        setRequestError(undefined);
        try {
            await submit(updatedParameters);
        } catch (ex) {
            const errorWidget = registerError(
                "LinkageRuleConfig-save-config",
                t("widget.LinkingRuleConfigWidget.saveError"),
                ex,
                { errorNotificationInstanceId: "_none_" },
            );
            setRequestError(errorWidget || undefined);
        }
        setSaving(false);
    };

    return (
        <SimpleDialog
            data-test-id={"clone-item-to-modal"}
            size="small"
            title={t("widget.LinkingRuleConfigWidget.modal.title")}
            isOpen={true}
            onClose={onClose}
            notifications={requestError}
            actions={[
                <Button
                    key="submit"
                    affirmative
                    onClick={onSubmit}
                    loading={saving}
                    disabled={!changed || errorCount > 0}
                    data-test-id={"linkage-rule-config-modal-submit-btn"}
                >
                    {t("common.action.update")}
                </Button>,
                <Button key="cancel" onClick={onClose}>
                    {t("common.action.cancel")}
                </Button>,
            ]}
        >
            {parameters.map((p) => {
                const errorMessage = errors.get(p.id);
                return (
                    <FieldItem
                        key={p.id}
                        labelProps={{
                            htmlFor: p.id,
                            text: p.label,
                        }}
                        intent={!!errorMessage ? "danger" : undefined}
                        messageText={errorMessage ? errorMessage : undefined}
                        helperText={p.description}
                    >
                        {p.type === "boolean" ? (
                            <Switch
                                id={"parameter-" + p.id}
                                defaultChecked={p.value === "true"}
                                onChange={(value: boolean) => changeParameter(p.id)(`${value}`)}
                            />
                        ) : p.onSearch ? (
                            <DefaultAutoCompleteField
                                id={"parameter-" + p.id}
                                initialValue={{ value: p.value ?? "" }}
                                onChange={changeParameter(p.id)}
                                onSearch={wrapAutoCompleteRequest(p.label, p.onSearch)}
                            />
                        ) : (
                            <TextField
                                data-test-id={"parameter-" + p.id}
                                onChange={changeParameterFromEvent(p.id)}
                                defaultValue={p.value}
                                placeholder={p.placeholder}
                            />
                        )}
                    </FieldItem>
                );
            })}
        </SimpleDialog>
    );
};
