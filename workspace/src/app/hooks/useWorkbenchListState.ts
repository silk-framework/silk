import React from "react";
import { useDispatch, useSelector } from "react-redux";
import { workspaceOp, workspaceSel } from "@ducks/workspace";
import { AppDispatch } from "store/configureStore";
import { GlobalTableContext } from "../GlobalContextsWrapper";
import { GlobalTableTypes, WorkbenchViewMode } from "./useStoreGlobalTableSettings";
import { useSelectFirstResult } from "./useSelectFirstResult";

export interface UseWorkbenchListStateResult {
    /** The applied text query, held back until the mount effect below has run (see the FIXME). */
    effectiveSearchQuery: string;
    error: ReturnType<typeof workspaceSel.errorSelector>;
    viewMode: WorkbenchViewMode;
    handleSearch: (textQuery: string) => void;
    handleViewModeChange: (mode: WorkbenchViewMode) => void;
    onEnter: () => void;
}

/**
 * Shared state/wiring for the workbench-style item list pages (`/workbench`, the Project "Contents"
 * tile, Activities): the applied search query, the surface's view mode, and the search / view-mode
 * change handlers.
 *
 * `tableKey` selects which `GlobalTableContext` slot (e.g. `"workbench"` vs `"projectContents"`)
 * this surface's view mode / page size / sort settings live under, so surfaces sharing this hook
 * don't clobber each other's persisted preferences.
 */
export function useWorkbenchListState(tableKey: GlobalTableTypes): UseWorkbenchListStateResult {
    const dispatch = useDispatch<AppDispatch>();
    const { textQuery } = useSelector(workspaceSel.appliedFiltersSelector);
    const error = useSelector(workspaceSel.errorSelector);
    const { globalTableSettings, updateGlobalTableSettings } = React.useContext(GlobalTableContext);
    const viewMode: WorkbenchViewMode = globalTableSettings[tableKey].viewMode ?? "table";
    const { onEnter } = useSelectFirstResult();

    // FIXME: Workaround to prevent search with a text query from another page sharing the same Redux state. Needs refactoring.
    const [searchInitialized, setSearchInitialized] = React.useState(false);
    const effectiveSearchQuery = searchInitialized ? textQuery : "";

    React.useEffect(() => {
        setSearchInitialized(true);
    }, []);

    const handleSearch = React.useCallback(
        (query: string) => {
            dispatch(workspaceOp.applyFiltersOp({ textQuery: query }));
        },
        [dispatch],
    );

    const handleViewModeChange = React.useCallback(
        (mode: WorkbenchViewMode) => {
            updateGlobalTableSettings({ viewMode: mode }, tableKey);
        },
        [updateGlobalTableSettings, tableKey],
    );

    return { effectiveSearchQuery, error, viewMode, handleSearch, handleViewModeChange, onEnter };
}
