import { RadioButton } from "@wrappers/index";
import React from "react";

export type FileMenuItems = "SELECT" | "NEW" | "EMPTY";

interface IProps {
    selectedFileMenu: FileMenuItems;

    onChange(value: string);
}

export function FileMenu({ onChange, selectedFileMenu }: IProps) {
    const menuItems = [
        {
            label: "Select file from project upload",
            value: "SELECT",
        },
        {
            label: "Upload new file",
            value: "NEW",
        },
        {
            label: "Start without data(create empty file)",
            value: "EMPTY",
        },
    ];

    const handleSelectChange = (e) => {
        const { value } = e.target;
        onChange(value);
    };

    return (
        <div>
            {menuItems.map((item) => (
                <RadioButton
                    key={item.value}
                    checked={selectedFileMenu === item.value}
                    label={item.label}
                    onChange={handleSelectChange}
                    value={item.value}
                />
            ))}
        </div>
    );
}
