import React from "react";
import { Button, FieldItem, FieldItemRow, Spacing, TextField } from "@wrappers/index";

const PrefixNew = ({ onAdd, onChangePrefix, prefix }) => {
    return (
        <fieldset>
            <legend>Add prefix</legend>
            <Spacing size="small" />
            <FieldItemRow>
                <FieldItem
                    key={"prefix-name"}
                    labelAttributes={{
                        htmlFor: "prefix-name",
                        text: "Prefix",
                    }}
                >
                    <TextField
                        id={"prefix-name"}
                        value={prefix.prefixName}
                        onChange={(e) => onChangePrefix("prefixName", e.target.value)}
                    />
                </FieldItem>
                <FieldItem
                    key={"prefix-name"}
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
                        Add
                    </Button>
                </FieldItem>
            </FieldItemRow>
        </fieldset>
    );
};

export default PrefixNew;
