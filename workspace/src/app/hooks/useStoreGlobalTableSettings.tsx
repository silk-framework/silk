import { workspaceOp } from "@ducks/workspace";
import { ISorterListItemState, SortModifierType } from "@ducks/workspace/typings";
import React from "react";
import { batch, useDispatch } from "react-redux";

const defaultConfig = {
    pageSize: 10,
    sortBy: "",
    sortOrder: "ASC",
};

const defaultGlobalTableSettings = {
    activities: { ...defaultConfig, pageSize: 25 },
    workbench: { ...defaultConfig },
    files: { ...defaultConfig, pageSize: 5 },
} as const;

type BaseConfig = {
    pageSize?: number;
    sortBy?: string;
    sortOrder?: SortModifierType;
};

interface GlobalTableSettings {
    activities: BaseConfig;
    workbench: BaseConfig;
    files: BaseConfig;
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

export const useStoreGlobalTableSettings = ({ key }: { key?: string } = {}) => {
    const dispatch = useDispatch();

    const getGlobalTableSettings = React.useCallback(() => {
        const storedSettings = localStorage.getItem(LOCAL_STORAGE_KEYS.GLOBAL_TABLE_SETTINGS);
        const tableSettings = !storedSettings ? defaultGlobalTableSettings : JSON.parse(storedSettings);
        return tableSettings;
    }, []);
    const storeKey = location.pathname.split("/").slice(-1)[0] === "activities" ? "activities" : "workbench";
    const pathname = key || storeKey;

    React.useEffect(() => {
        updateGlobalTableSettings(getGlobalTableSettings()[pathname]);
    }, [pathname, getGlobalTableSettings]);

    const updateGlobalTableSettings = React.useCallback(
        (settings: BaseConfig) => {
            const globalTableSettings = getGlobalTableSettings();
            const newSettings: GlobalTableSettings = {
                ...globalTableSettings,
                [pathname]: {
                    ...globalTableSettings[pathname],
                    ...settings,
                },
            };
            localStorage.setItem(LOCAL_STORAGE_KEYS.GLOBAL_TABLE_SETTINGS, JSON.stringify(newSettings));
            const { sortBy, pageSize, sortOrder } = newSettings[pathname as settingsConfig["path"]];
            dispatch(workspaceOp.applySorterOp(sortBy!, sortOrder));
            dispatch(workspaceOp.changeLimitOp(pageSize!));
        },
        [getGlobalTableSettings, pathname],
    );

    return {
        updateGlobalTableSettings,
        getGlobalTableSettings,
    };
};
