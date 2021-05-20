import React, { useEffect, useState } from "react";
import { Button, FieldItem, FieldItemRow, FieldSet, Icon, TextField } from "@gui-elements/index";
import { useTranslation } from "react-i18next";
import { IPrefixDefinition } from "@ducks/workspace/typings";

interface IProps {
    onAdd: (prefixDefinition: IPrefixDefinition) => any;
}

/** From https://www.w3.org/TR/turtle/#grammar-production-PN_PREFIX */
export const createPrefixNameRegex = (): RegExp => {
    const charsBase =
        "(?:[A-Z]|[a-z]|[\\u00C0-\\u00D6]|[\\u00D8-\\u00F6]|[\\u00F8-\\u02FF]|[\\u0370-\\u037D]|[\\u037F-\\u1FFF]|[\\u200C-\\u200D]|[\\u2070-\\u218F]|[\\u2C00-\\u2FEF]|[\\u3001-\\uD7FF]|[\\uF900-\\uFDCF]|[\\uFDF0-\\uFFFD])";
    const chars = `(?:${charsBase}|_|-|[0-9]|\\u00B7|[\\u0300-\\u036F]|[\\u203F-\\u2040])`;
    return new RegExp(`^${charsBase}(?:(?:${chars}|\\.)*${chars})?$`);
};

const invalidUriChars = new RegExp('[\\u0000-\\u0020<>"{}|^`\\\\]');

export const prefixNameRegex = createPrefixNameRegex();

export const validatePrefixName = (prefixName: string): boolean => prefixNameRegex.test(prefixName);

/** Returns either a boolean with true being a valid value or a number that specifies the first index with an invalid character. */
export const validatePrefixValue = (prefixValue: string): boolean | number => {
    try {
        const invalidCharMatches = prefixValue.match(invalidUriChars);
        if (invalidCharMatches) {
            return invalidCharMatches.index;
        } else {
            const uri = new URL(prefixValue);
            return !!(uri.host || uri.pathname);
        }
    } catch (ex) {
        return false;
    }
};

/** Component for entering a new prefix. */
const PrefixNew = ({ onAdd }: IProps) => {
    const [t] = useTranslation();
    const [prefixDefinition, setPrefixDefinition] = useState<IPrefixDefinition>({ prefixName: "", prefixUri: "" });
    const [isValidPrefixName, setIsValidPrefixName] = useState<boolean>(false);
    // The prefix value is invalid when the value is false or a number, which specifies the character index of the first invalid char.
    const [isValidPrefixValue, setIsValidPrefixValue] = useState<boolean | number>(false);

    useEffect(() => {}, [prefixDefinition.prefixName + prefixDefinition.prefixUri]);

    const onPrefixNameChange = (e) => {
        const value = e?.target?.value;
        if (value != null) {
            setPrefixDefinition((currentDefinition) => {
                return { ...currentDefinition, prefixName: value };
            });
            setIsValidPrefixName(validatePrefixName(value));
        }
    };
    const onPrefixUriChange = (e) => {
        const value = e?.target?.value;
        if (value != null) {
            setPrefixDefinition((currentDefinition) => {
                return { ...currentDefinition, prefixUri: value };
            });
            setIsValidPrefixValue(validatePrefixValue(value));
        }
    };

    let prefixNameErrorIcon: JSX.Element | undefined = undefined;
    let prefixValueErrorIcon: JSX.Element | undefined = undefined;

    if (!isValidPrefixName && prefixDefinition.prefixName) {
        prefixNameErrorIcon = <Icon name={"state-danger"} tooltipText={t("PrefixDialog.prefixNameInvalid")} />;
    }
    const highlightInvalidPrefixValue =
        (typeof isValidPrefixValue == "number" || !isValidPrefixValue) && prefixDefinition.prefixUri;
    if (highlightInvalidPrefixValue) {
        let tooltipText = t("PrefixDialog.prefixUriInvalid");
        if (typeof isValidPrefixValue == "number" && isValidPrefixValue < prefixDefinition.prefixUri.length) {
            tooltipText = t("PrefixDialog.prefixUriInvalidChar", {
                idx: isValidPrefixValue,
                char: prefixDefinition.prefixUri.substr(isValidPrefixValue, 1),
            });
        }
        prefixValueErrorIcon = <Icon name={"state-danger"} tooltipText={tooltipText} />;
    }

    return (
        <FieldSet title={t("common.action.AddSmth", { smth: t("widget.ConfigWidget.prefix") })} boxed>
            <FieldItemRow>
                <FieldItem
                    key={"prefix-name"}
                    labelAttributes={{
                        htmlFor: "prefix-name",
                        text: t("widget.ConfigWidget.prefix", "Prefix"),
                    }}
                >
                    <TextField
                        id={"prefix-name"}
                        value={prefixDefinition.prefixName}
                        onChange={onPrefixNameChange}
                        leftIcon={prefixNameErrorIcon}
                        hasStateDanger={!isValidPrefixName && prefixDefinition.prefixName}
                    />
                </FieldItem>
                <FieldItem
                    key={"prefix-uri"}
                    labelAttributes={{
                        htmlFor: "prefix-uri",
                        text: "URI",
                    }}
                    hasStateDanger={!isValidPrefixValue && prefixDefinition.prefixUri}
                >
                    <TextField
                        id={"prefix-uri"}
                        value={prefixDefinition.prefixUri}
                        onChange={onPrefixUriChange}
                        leftIcon={prefixValueErrorIcon}
                    />
                </FieldItem>
                <FieldItem key={"prefix-submit"}>
                    <Button
                        onClick={() => onAdd(prefixDefinition)}
                        elevated
                        disabled={!isValidPrefixName || !isValidPrefixValue || typeof isValidPrefixValue == "number"}
                    >
                        {t("common.action.add", "Add")}
                    </Button>
                </FieldItem>
            </FieldItemRow>
        </FieldSet>
    );
};

export default PrefixNew;
