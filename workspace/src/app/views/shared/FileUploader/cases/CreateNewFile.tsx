import { FieldItem, TextField } from "@gui-elements/index";
import React, { useState } from "react";

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
            labelAttributes={{
                text: "New file name",
                info: "required",
                htmlFor: "fileInput",
            }}
            messageText={error ? "File name not specified" : ""}
        >
            <TextField
                id="fileInput"
                name="fileInput"
                onChange={handleNewFileNameChange}
                onBlur={() => onBlur(newFileName)}
                value={newFileName}
                placeholder={""}
                hasStateDanger={error}
                hasStateSuccess={!error && newFileName}
            />
        </FieldItem>
    );
}
