import React, {useEffect, useState} from 'react';
import {IFacetState} from "@ducks/workspace/typings";
import {useDispatch, useSelector} from "react-redux";
import {workspaceOp, workspaceSel} from "@ducks/workspace";
import Label from "@wrappers/label";
import FacetItem from "./FacetItem";
import Popover from "@wrappers/popover";
import Tooltip from "@wrappers/tooltip";
import {Classes, Position} from "@wrappers/constants";

export default function FacetsList() {
    const dispatch = useDispatch();

    const facets = useSelector(workspaceSel.facetsSelector);
    const appliedFacets = useSelector(workspaceSel.appliedFacetsSelector);

    const [visibleFacetsKeywords, setVisibleFacetsKeywords] = useState({});
    const [toggledFacets, setToggledFacets] = useState([]);

    const FACETS_PREVIEW_LIMIT = 5;

    useEffect(() => {
        const visiblesOnly = {} as any;
        facets.forEach(facet => {
            visiblesOnly[facet.id] = facet.values.slice(0, FACETS_PREVIEW_LIMIT);
        });
        setVisibleFacetsKeywords(visiblesOnly);
    }, [facets]);

    const isChecked = (facetId: string, value: string): boolean => {
        const existsFacet = appliedFacets.find(o => o.facetId === facetId);
        if (!existsFacet) {
            return false;
        }
        return existsFacet.keywordIds.includes(value);
    };

    const handleSetFacet = (facet: IFacetState, value: string) => {
        dispatch(workspaceOp.toggleFacetOp(facet, value));
    };

    const toggleShowMore = (facet: IFacetState) => {
        const toggledIndex = toggledFacets.findIndex(f => f === facet.id);
        const keywords = {...visibleFacetsKeywords};

        if (toggledIndex > -1) {
            keywords[facet.id] = [...facet.values.slice(0, FACETS_PREVIEW_LIMIT)];
            toggledFacets.splice(toggledIndex, 1);
        } else {
            keywords[facet.id] = [...facet.values];
            toggledFacets.push(facet.id);
        }
        setToggledFacets(toggledFacets);
        setVisibleFacetsKeywords(keywords);
    };

    return (
        <div>
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
                            visibleFacetsKeywords[facet.id] && visibleFacetsKeywords[facet.id].map(val =>
                                <FacetItem
                                    key={val.id}
                                    isChecked={isChecked(facet.id, val.id)}
                                    value={val.id}
                                    onSelectFacet={(valueId) => handleSetFacet(facet, valueId)}
                                    label={`${val.label} (${val.count})`}
                                />
                            )
                        }
                        <a onClick={() => toggleShowMore(facet)}>{(visibleFacetsKeywords[facet.id] && visibleFacetsKeywords[facet.id].length > FACETS_PREVIEW_LIMIT) ? 'Show less...' : 'Show more...'}</a>
                    </div>
                )
            }
        </div>
    )
}
