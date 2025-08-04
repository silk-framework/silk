import { workspaceOp } from "@ducks/workspace";
import { ISorterListItemState } from "@ducks/workspace/typings";
import React from "react";
import { batch, useDispatch } from "react-redux";

const defaultConfig = {
    pageSize: 10,
    sortBy: "",
};

const defaultGlobalTableSettings = {
    activities: { ...defaultConfig, pageSize: 25 },
    workbench: { ...defaultConfig },
    files: { ...defaultConfig, pageSize: 5 },
} as const;

type BaseConfig = {
    pageSize?: number;
    sortBy?: string;
};

interface GlobalTableSettings {
    activities: BaseConfig;
    workbench: BaseConfig;
    files: BaseConfig;
}

const LOCAL_STORAGE_KEYS = {
    GLOBAL_TABLE_SETTINGS: "global_table_settings",
};

type settingsConfig = {
    sorters: ISorterListItemState[];
    activeSortBy: string;
    onSort: (sortBy: string) => void;
    path: keyof typeof defaultGlobalTableSettings;
};

export const useStoreGlobalTableSettings = (
    config: settingsConfig = { sorters: [], activeSortBy: "", onSort: () => {}, path: "workbench" } as settingsConfig,
) => {
    const [globalTableSettings, setGlobalTableSettings] = React.useState<GlobalTableSettings>(() => {
        const storedSettings = localStorage.getItem(LOCAL_STORAGE_KEYS.GLOBAL_TABLE_SETTINGS);
        if (!storedSettings) return defaultGlobalTableSettings;
        return JSON.parse(storedSettings);
    });
    const dispatch = useDispatch();

    React.useEffect(() => {
        //don't need to track 'config' because every change resets the globalTableSettings, it's re-rendered
        // page change still has old redux state, but is reset in Workspace.tsx etc. do work after
        setTimeout(() => {
            batch(() => {
                const { sortBy = "", pageSize } =
                    (globalTableSettings ?? defaultGlobalTableSettings)[config.path] || defaultConfig;
                const validSorter = config.sorters.find((s) => s.id === sortBy);
                if (sortBy && validSorter && config.activeSortBy !== sortBy) {
                    //has valid sortBy filter for this kind of list
                    config.onSort(sortBy);
                } else if (!validSorter) {
                    config.onSort("");
                }
                updateGlobalTableSettings({ [config.path]: { sortBy, pageSize } });
                pageSize && dispatch(workspaceOp.changeLimitOp(pageSize));
            });
        });
    }, []);

    const updateGlobalTableSettings = React.useCallback(
        (settings: Partial<GlobalTableSettings>) => {
            const newSettings = { ...globalTableSettings, ...settings };
            setGlobalTableSettings(newSettings as GlobalTableSettings);
            localStorage.setItem(LOCAL_STORAGE_KEYS.GLOBAL_TABLE_SETTINGS, JSON.stringify(newSettings));
        },
        [globalTableSettings],
    );

    return {
        globalTableSettings,
        updateGlobalTableSettings,
    };
};
