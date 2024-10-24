import { FieldItem, TextField } from "@eccenca/gui-elements";
import React, { useState } from "react";
import { useTranslation } from "react-i18next";

interface IProps {
    /**
     * Fire when input changed
     * @param value
     */
    onChange(value: string);

    onBlur?(value: string);

    /**
     * Show Change button for extra confirmation
     */
    confirmationButton?: boolean;
}

/**
 * The Widget for create new file option
 * @constructor
 */
export function CreateNewFile(props: IProps) {
    const { onChange, onBlur } = props;

    const [newFileName, setNewFileName] = useState("");
    const [error, setError] = useState(false);
    const [t] = useTranslation();

    const handleNewFileNameChange = (e) => {
        const { value } = e.target;
        if (value || newFileName) {
            setError(false);
        } else if (!value) {
            setError(true);
        }
        setNewFileName(value);
        onChange(value);
    };

    return (
        <FieldItem
            labelProps={{
                text: t("FileUploader.createNewFile", "New file name"),
                info: t("common.words.required"),
                htmlFor: "fileInput",
            }}
            messageText={error ? t("FileUploader.fileNotSpecified", "File name not specified") : ""}
        >
            <TextField
                id="fileInput"
                name="fileInput"
                onChange={handleNewFileNameChange}
                onBlur={() => onBlur?.(newFileName)}
                value={newFileName}
                placeholder={""}
                intent={error ? "danger" : !error && !!newFileName ? "success" : undefined}
            />
        </FieldItem>
    );
}
