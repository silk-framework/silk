import { FieldItem, TextField } from "@wrappers/index";
import React, { useState } from "react";

interface IProps {
    /**
     * Fire when input changed
     * @param value
     */
    onChange(value: string);

    onBlur?(value: string);
}

/**
 * The Widget for create new file option
 * @param onChange
 * @constructor
 */
export function CreateNewFile({ onChange, onBlur }: IProps) {
    const [newFileName, setNewFileName] = useState("");
    const [error, setError] = useState(false);

    const handleNewFileNameChange = (e) => {
        const { value } = e.target;
        if (value || !newFileName) {
            setError(false);
        } else if (this.state.newFileName) {
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
                placeholder="Write new file name"
                hasStateDanger={error}
                hasStateSuccess={!error && newFileName}
            />
        </FieldItem>
    );
}
