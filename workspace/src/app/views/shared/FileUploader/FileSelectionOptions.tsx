import { RadioButton, FieldItemRow, FieldItem } from "gui-elements";
import React from "react";
import { useTranslation } from "react-i18next";

export type FileMenuItems = "SELECT" | "NEW" | "EMPTY";

interface IProps {
    selectedFileMenu: FileMenuItems;

    onChange(value: string);
}

/**
 * Option menu to pick from where the file should be selected. It comes with 3 options: New (upload), Select (existing), Empty
 * @param onChange
 * @param selectedFileMenu
 * @constructor
 */
export function FileSelectionOptions({ onChange, selectedFileMenu }: IProps) {
    const [t] = useTranslation();

    const menuItems = [
        {
            label: t("FileMenu.empty", "Select file from project"),
            value: "SELECT",
        },
        {
            label: t("FileMenu.new", "Upload new file"),
            value: "NEW",
        },
        {
            label: t("FileMenu.create", "Create empty file"),
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
