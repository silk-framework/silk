import React from 'react';
import { IFacetState } from "@ducks/dashboard/typings";
import { useDispatch, useSelector } from "react-redux";
import { dashboardOp, dashboardSel } from "@ducks/dashboard";
import Label from "@wrappers/label";
import FacetItem from "./FacetItem";
import Popover from "@wrappers/popover";
import Tooltip from "@wrappers/tooltip";
import { Classes, Position } from "@wrappers/constants";

export default function FacetsList() {
    const dispatch = useDispatch();

    const facets = useSelector(dashboardSel.facetsSelector);
    const appliedFacets = useSelector(dashboardSel.appliedFacetsSelector);

    const isChecked = (facetId: string, value: string): boolean => {
        const existsFacet = appliedFacets.find(o => o.facetId === facetId);
        if (!existsFacet) {
            return false;
        }
        return existsFacet.keywordIds.includes(value);
    };

    const handleSetFacet = (facet: IFacetState, value: string) => {
        dispatch(dashboardOp.toggleFacetOp(facet, value));
    };

    return (
        <div style={{marginTop: '10px'}}>
            {
                facets.map(facet =>
                    <div key={facet.id}>
                        <Popover position={Position.BOTTOM_LEFT}>
                            <Tooltip
                                className={Classes.TOOLTIP_INDICATOR}
                                content={facet.description}
                                position={Position.BOTTOM_LEFT}
                            >
                                <Label>{facet.label}</Label>
                            </Tooltip>
                        </Popover>
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
