import { workspaceOp } from "@ducks/workspace";
import { ISorterListItemState } from "@ducks/workspace/typings";
import React from "react";
import { batch, useDispatch } from "react-redux";

interface GlobalTableSettings {
    pageSize: number;
    sortBy: string;
}

const LOCAL_STORAGE_KEYS = {
    GLOBAL_TABLE_SETTINGS: "global_table_settings",
};

type settingsConfig = {
    sorters: ISorterListItemState[];
    activeSortBy: string;
    onSort: (sortBy: string) => void;
};

export const useStoreGlobalTableSettings = (
    config: settingsConfig = { sorters: [], activeSortBy: "", onSort: () => {} },
) => {
    const [globalTableSettings, setGlobalTableSettings] = React.useState<GlobalTableSettings | null>(() => {
        const storedSettings = localStorage.getItem(LOCAL_STORAGE_KEYS.GLOBAL_TABLE_SETTINGS);
        if (!storedSettings) return null;
        return JSON.parse(storedSettings);
    });
    const dispatch = useDispatch();

    React.useEffect(() => {
        //don't need to track 'config' because every change resets the globalTableSettings, it's re-rendered
        // page change still has old redux state, but is reset in Workspace.tsx etc. do work after
        setTimeout(() => {
            batch(() => {
                const { sortBy = "", pageSize } = globalTableSettings ?? {};
                const validSorter = config.sorters.find((s) => s.id === sortBy);
                if (sortBy && validSorter && config.activeSortBy !== sortBy) {
                    //has valid sortBy filter for this kind of list
                    config.onSort(sortBy);
                } else if (!validSorter) {
                    config.onSort("");
                }
                updateGlobalTableSettings({ sortBy });
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
