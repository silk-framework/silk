import { workspaceOp } from "@ducks/workspace";
import { ISorterListItemState, SortModifierType } from "@ducks/workspace/typings";
import React from "react";
import { batch, useDispatch } from "react-redux";

const defaultConfig: GlobalTableBaseConfig = {
    pageSize: 10,
    sortBy: "",
    sortOrder: "ASC",
};

export const defaultGlobalTableSettings: GlobalTableSettings = {
    activities: { ...defaultConfig, pageSize: 25 },
    workbench: { ...defaultConfig },
    files: { ...defaultConfig, pageSize: 5 },
} as const;

export type GlobalTableBaseConfig = {
    pageSize?: number;
    sortBy?: string;
    sortOrder?: SortModifierType;
};

export interface GlobalTableSettings {
    activities: GlobalTableBaseConfig;
    workbench: GlobalTableBaseConfig;
    files: GlobalTableBaseConfig;
}

const LOCAL_STORAGE_KEYS = {
    GLOBAL_TABLE_SETTINGS: "global_table_settings",
};

export type settingsConfig = {
    sorters: ISorterListItemState[];
    activeSortBy: string;
    onSort: (sortBy: string) => void;
    path: keyof typeof defaultGlobalTableSettings;
};

export type GlobalTableTypes = "workbench" | "files" | "activities"

interface GlobalTableSettingFunctions {
    /** Set the table settings for a specific global table. Specify table explicitly, else it will be derived from the path if possible. */
    updateGlobalTableSettings: (settings: GlobalTableBaseConfig, explicitKey?: GlobalTableTypes) => void;
    globalTableSettings: GlobalTableSettings
}

export const useStoreGlobalTableSettings: () => GlobalTableSettingFunctions = () => {
    // Return the current global settings from local storage or if not existing default values
    const getGlobalTableSettings = React.useCallback(() => {
        const storedSettings = localStorage.getItem(LOCAL_STORAGE_KEYS.GLOBAL_TABLE_SETTINGS);
        return !storedSettings ? defaultGlobalTableSettings : JSON.parse(storedSettings);
    }, []);
    const [globalTableSettings, setGlobalTableSettings] = React.useState(getGlobalTableSettings)

    // Extracts the table key from the location path
    const extractTableKey = (): GlobalTableTypes => {
        return location.pathname.split("/").slice(-1)[0] === "activities" ? "activities" : "workbench";
    }
    const updateGlobalTableSettings = React.useCallback(
        (settings: GlobalTableBaseConfig, customKey?: GlobalTableTypes) => {
            const tableKey = customKey ?? extractTableKey()
            const globalTableSettings = getGlobalTableSettings();
            const newSettings: GlobalTableSettings = {
                ...globalTableSettings,
                [tableKey]: {
                    ...globalTableSettings[tableKey],
                    ...settings,
                },
            };
            const {sortBy: lastSortBy, sortOrder: lastSortOrder} = globalTableSettings[tableKey];
            const {sortBy, sortOrder} = newSettings[tableKey as settingsConfig["path"]];
            let newSortOrder: SortModifierType = sortOrder || "ASC";
            if (lastSortBy === sortBy) {
                newSortOrder = lastSortOrder === "ASC" ? "DESC" : "ASC";
            }
            newSettings[tableKey].sortOrder = newSortOrder;
            localStorage.setItem(LOCAL_STORAGE_KEYS.GLOBAL_TABLE_SETTINGS, JSON.stringify(newSettings));
            setGlobalTableSettings(newSettings)
        },
        [getGlobalTableSettings]);

    return {
        updateGlobalTableSettings,
        globalTableSettings
    };
};
