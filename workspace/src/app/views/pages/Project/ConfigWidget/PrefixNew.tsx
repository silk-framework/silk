import React from "react";
import { Button, FieldItem, FieldItemRow, FieldSet, TextField } from "@wrappers/index";
import { useTranslation } from "react-i18next";

const PrefixNew = ({ onAdd, onChangePrefix, prefix }) => {
    const [t] = useTranslation();

    return (
        <FieldSet title={t("AddSmth", { smth: t("widget.config.prefix") })} boxed>
            <FieldItemRow>
                <FieldItem
                    key={"prefix-name"}
                    labelAttributes={{
                        htmlFor: "prefix-name",
                        text: t("widget.config.prefix", "Prefix"),
                    }}
                >
                    <TextField
                        id={"prefix-name"}
                        value={prefix.prefixName}
                        onChange={(e) => onChangePrefix("prefixName", e.target.value)}
                    />
                </FieldItem>
                <FieldItem
                    key={"prefix-uri"}
                    labelAttributes={{
                        htmlFor: "prefix-uri",
                        text: "URI",
                    }}
                >
                    <TextField
                        id={"prefix-uri"}
                        value={prefix.prefixUri}
                        onChange={(e) => onChangePrefix("prefixUri", e.target.value)}
                    />
                </FieldItem>
                <FieldItem key={"prefix-submit"}>
                    <Button onClick={onAdd} elevated disabled={!prefix.prefixName || !prefix.prefixUri}>
                        {t("common.action.add", "Add")}
                    </Button>
                </FieldItem>
            </FieldItemRow>
        </FieldSet>
    );
};

export default PrefixNew;
