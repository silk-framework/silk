import React from "react";
import { useSelector } from "react-redux";
import { workspaceSel } from "@ducks/workspace";
import { IAppliedSorterState } from "@ducks/workspace/typings";
import SearchBar from "../../../shared/SearchBar";
import SortButton from "../../../shared/buttons/SortButton";
import { GlobalTableContext } from "../../../../GlobalContextsWrapper";
import { WorkbenchViewMode } from "../../../../hooks/useStoreGlobalTableSettings";
import ViewModeToggle from "./ViewModeToggle";
import WorkspaceFilters from "./WorkspaceFilters";

interface IProps {
    textQuery: string;
    onSearch(textQuery: string): void;
    onEnter?(): void;
    viewMode: WorkbenchViewMode;
    onViewModeChange(mode: WorkbenchViewMode): void;
    /** When rendered inside a project, hides the project-specific filter options. */
    projectId?: string;
    /** Focus the search field on mount. Defaults to true (the `/workbench` behavior); embedded
     *  usages (e.g. the project "Contents" tile) should pass false to avoid stealing the focus. */
    focusOnCreation?: boolean;
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
    focusOnCreation = true,
}: IProps) {
    const sorters = useSelector(workspaceSel.sortersSelector);
    const { globalTableSettings } = React.useContext(GlobalTableContext);

    // Merge the persisted sort (localStorage) over the server-provided applied sort, mirroring SearchBar.
    const appliedSorters: IAppliedSorterState | undefined = sorters ? { ...sorters.applied } : undefined;
    const conf = globalTableSettings["workbench"];
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
                    globalTableKey={"workbench"}
                />
            </div>
            <WorkspaceFilters projectId={projectId} />
            <div className="ml-auto flex items-center gap-2">
                {!!sorters && !!sorters.list.length && appliedSorters && (
                    <SortButton sortersList={sorters.list} activeSort={appliedSorters} />
                )}
                <ViewModeToggle mode={viewMode} onChange={onViewModeChange} />
            </div>
        </div>
    );
}
