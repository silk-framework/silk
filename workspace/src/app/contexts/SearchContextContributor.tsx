import { workspaceSel } from "@ducks/workspace";
import React from "react";
import { useSelector } from "react-redux";

import { createSearchContext, usePageContextContribution } from "./ApplicationContextRegistry";

interface SearchContextContributorProps {
    enabled: boolean;
    projectId?: string;
    view: "project" | "workspace";
}

export const SearchContextContributor = React.memo(
    ({ enabled, projectId, view }: SearchContextContributorProps) => {
        const appliedFilters = useSelector(workspaceSel.appliedFiltersSelector);
        const appliedFacets = useSelector(workspaceSel.appliedFacetsSelector);
        const availableFacets = useSelector(workspaceSel.facetsSelector);
        const search = React.useMemo(
            () => createSearchContext(appliedFilters, appliedFacets, availableFacets),
            [appliedFacets, appliedFilters, availableFacets],
        );
        const contribution = React.useMemo(
            () => {
                const scope =
                    view === "workspace"
                        ? ({ view } as const)
                        : projectId
                          ? ({ projectId, view } as const)
                          : undefined;
                return enabled && scope ? { kind: "search" as const, scope, search } : undefined;
            },
            [enabled, projectId, search, view],
        );
        usePageContextContribution(contribution);
        return null;
    },
);
