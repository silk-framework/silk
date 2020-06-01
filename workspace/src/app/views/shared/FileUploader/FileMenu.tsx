import { RadioButton, FieldItemRow, FieldItem } from "@wrappers/index";
import React from "react";

export type FileMenuItems = "SELECT" | "NEW" | "EMPTY";

interface IProps {
    selectedFileMenu: FileMenuItems;

    onChange(value: string);
}

/**
 * File widget menu with 3 options: New, Select, Empty
 * @param onChange
 * @param selectedFileMenu
 * @constructor
 */
export function FileMenu({ onChange, selectedFileMenu }: IProps) {
    const menuItems = [
        {
            label: "Select file from project",
            value: "SELECT",
        },
        {
            label: "Upload new file",
            value: "NEW",
        },
        {
            label: "Create empty file",
            value: "EMPTY",
        },
    ];

    const handleSelectChange = (e) => {
        const { value } = e.target;
        onChange(value);
    };

    return (
        <FieldItemRow>
            {menuItems.map((item) => (
                <FieldItem key={item.value}>
                    <RadioButton
                        checked={selectedFileMenu === item.value}
                        label={item.label}
                        onChange={handleSelectChange}
                        value={item.value}
                    />
                </FieldItem>
            ))}
        </FieldItemRow>
    );
}
