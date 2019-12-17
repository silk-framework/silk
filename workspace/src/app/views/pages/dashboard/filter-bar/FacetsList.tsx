import React from 'react';
import { IFacetState } from "@ducks/dashboard/typings";
import { useDispatch, useSelector } from "react-redux";
import { dashboardOp, dashboardSel } from "@ducks/dashboard";
import Label from "@wrappers/label";
import FacetItem from "./FacetItem";

export default function FacetsList() {
    const dispatch = useDispatch();

    const facets = useSelector(dashboardSel.facetsSelector);
    const appliedFilters = useSelector(dashboardSel.appliedFiltersSelector);

    const isChecked = (facetId: string, value: string): boolean => {
        const existsFacet = appliedFilters.facets.find(o => o.facetId === facetId);
        if (!existsFacet) {
            return false;
        }
        return existsFacet.keywordIds.includes(value);
    };

    const handleSetFacet = (facet: IFacetState, value: string) => {
        dispatch(dashboardOp.applyFacet({
            facet,
            value
        }));
    };

    return (
        <div style={{marginTop: '10px'}}>
            {
                facets.map(facet =>
                    <div key={facet.id}>
                        <Label>{facet.label}</Label>
                        {
                            facet.values.map(val =>
                                <FacetItem
                                    key={val.id}
                                    isChecked={isChecked(facet.id, val.id)}
                                    value={val.id}
                                    onSelectFacet={(valueId) => handleSetFacet(facet, valueId)}
                                    label={`${val.label} (${val.count})`}
                                />
                            )
                        }
                    </div>
                )
            }
        </div>
    )
}
