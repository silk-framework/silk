import React from "react";
import { useSelector } from "react-redux";
import { workspaceSel } from "@ducks/workspace";
import { IAppliedSorterState } from "@ducks/workspace/typings";
import { IAvailableDataTypeOption } from "@ducks/common/typings";
import SearchBar from "../../../shared/SearchBar";
import SortButton from "../../../shared/buttons/SortButton";
import { GlobalTableContext } from "../../../../GlobalContextsWrapper";
import { GlobalTableTypes, WorkbenchViewMode } from "../../../../hooks/useStoreGlobalTableSettings";
import ViewModeToggle from "./ViewModeToggle";
import WorkspaceFilters from "./WorkspaceFilters";

interface IProps {
    textQuery: string;
    onSearch(textQuery: string): void;
    onEnter?(): void;
    /** The current view mode, rendered as a table/grid toggle on the right. Omit on surfaces that
     *  have a single fixed presentation (e.g. Activities), which hides the toggle. */
    viewMode?: WorkbenchViewMode;
    onViewModeChange?(mode: WorkbenchViewMode): void;
    /** When rendered inside a project, hides the project-specific filter options. */
    projectId?: string;
    /** Additional item-type options prepended to the server-provided ones (e.g. Activities' "Global"). */
    extraItemTypeModifiers?: IAvailableDataTypeOption[];
    /** Focus the search field on mount. Defaults to true (the `/workbench` behavior); embedded
     *  usages (e.g. the project "Contents" tile) should pass false to avoid stealing the focus. */
    focusOnCreation?: boolean;
    /** Which `GlobalTableContext` slot this toolbar's sort preference is stored under. Defaults to
     *  `"workbench"`; the Project "Contents" tile passes `"projectContents"`. */
    globalTableKey?: GlobalTableTypes;
    /** Extra controls rendered at the far right of the toolbar, after the sort/view controls
     *  (e.g. the Activities reload button). */
    actions?: React.ReactNode;
}

/**
 * The workbench search/filter toolbar: a compact search field on the top-left, the sort menu and
 * the table/grid view toggle on the top-right, and the filter dropdowns on the row below.
 */
export default function WorkspaceToolbar({
    textQuery,
    onSearch,
    onEnter,
    viewMode,
    onViewModeChange,
    projectId,
    extraItemTypeModifiers,
    focusOnCreation = true,
    globalTableKey = "workbench",
    actions,
}: IProps) {
    const sorters = useSelector(workspaceSel.sortersSelector);
    const { globalTableSettings } = React.useContext(GlobalTableContext);

    // Merge the persisted sort (localStorage) over the server-provided applied sort, mirroring SearchBar.
    const appliedSorters: IAppliedSorterState | undefined = sorters ? { ...sorters.applied } : undefined;
    const conf = globalTableSettings[globalTableKey];
    if (appliedSorters && conf) {
        if (conf.sortBy) {
            appliedSorters.sortBy = conf.sortBy;
        }
        if (conf.sortOrder) {
            appliedSorters.sortOrder = conf.sortOrder;
        }
    }

    return (
        <div className="flex flex-wrap items-center gap-2">
            <div className="w-full sm:w-72">
                <SearchBar
                    focusOnCreation={focusOnCreation}
                    textQuery={textQuery}
                    onSearch={onSearch}
                    onEnter={onEnter}
                    disableEnterDuringPendingSearch={true}
                    globalTableKey={globalTableKey}
                />
            </div>
            <WorkspaceFilters projectId={projectId} extraItemTypeModifiers={extraItemTypeModifiers} />
            <div className="ml-auto flex items-center gap-2">
                {!!sorters && !!sorters.list.length && appliedSorters && (
                    <SortButton sortersList={sorters.list} activeSort={appliedSorters} />
                )}
                {viewMode !== undefined && onViewModeChange && (
                    <ViewModeToggle mode={viewMode} onChange={onViewModeChange} />
                )}
                {actions}
            </div>
        </div>
    );
}
