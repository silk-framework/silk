import { workspaceOp } from "@ducks/workspace";
import { ISorterListItemState } from "@ducks/workspace/typings";
import { useLocation } from "react-router";
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
    sortOrder?: "ASC" | "DESC";
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

export const useStoreGlobalTableSettings = () => {
    const dispatch = useDispatch();
    const location = useLocation();
    const pathname = location.pathname.split("/").slice(-1)[0];

    const getGlobalTableSettings = React.useCallback(() => {
        const storedSettings = localStorage.getItem(LOCAL_STORAGE_KEYS.GLOBAL_TABLE_SETTINGS);
        const tableSettings = !storedSettings ? defaultGlobalTableSettings : JSON.parse(storedSettings);
        return tableSettings;
    }, []);

    React.useEffect(() => {
        //don't need to track 'config' because every change resets the globalTableSettings, it's re-rendered
        // page change still has old redux state, but is reset in Workspace.tsx etc. do work after
        setTimeout(() => {
            batch(() => {
                const globalTableSettings = getGlobalTableSettings();
                const {
                    sortBy = "",
                    pageSize,
                    sortOrder,
                } = (globalTableSettings ?? defaultGlobalTableSettings)[pathname] || defaultConfig;
                updateGlobalTableSettings({ sortBy, pageSize, sortOrder });
            });
        });
    }, []);

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
            const { sortBy, pageSize } = newSettings[pathname as settingsConfig["path"]];
            batch(() => {
                sortBy && dispatch(workspaceOp.applySorterOp(sortBy));
                pageSize && dispatch(workspaceOp.changeLimitOp(pageSize));
            });
        },
        [getGlobalTableSettings, pathname],
    );

    return {
        updateGlobalTableSettings,
    };
};
