import React, {useEffect, useState} from 'react';
import {IFacetState} from "@ducks/workspace/typings";
import {useDispatch, useSelector} from "react-redux";
import {workspaceOp, workspaceSel} from "@ducks/workspace";
import FacetItem from "./FacetItem";
import {
    Spacing,
    Tooltip,
    Button,
    TitleSubsection,
    Icon,
} from "@wrappers/index";

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
                        <TitleSubsection>
                            <h3>
                                {facet.label}
                                &nbsp;
                                <Tooltip
                                    content={facet.description}
                                >
                                    <Icon name="item-info" small />
                                </Tooltip>
                            </h3>
                        </TitleSubsection>
                        <Spacing size="tiny" />
                        <ul>
                            {
                                visibleFacetsKeywords[facet.id] && visibleFacetsKeywords[facet.id].map(val =>
                                    <li><FacetItem
                                        key={val.id}
                                        isChecked={isChecked(facet.id, val.id)}
                                        value={val.id}
                                        onSelectFacet={(valueId) => handleSetFacet(facet, valueId)}
                                        label={`${val.label} (${val.count})`}
                                    /></li>
                                )
                            }
                            {
                                facet.values.length <= FACETS_PREVIEW_LIMIT
                                ? null
                                : <li>
                                    <Button
                                        onClick={() => toggleShowMore(facet)}
                                        text={toggledFacets.includes(facet.id) ? 'show less' : 'show more'}
                                        rightIcon={toggledFacets.includes(facet.id) ? 'toggler-showless' : 'toggler-showmore'}
                                        small
                                        minimal
                                    />
                                </li>
                            }

                        </ul>
                        <Spacing />
                    </div>
                )
            }
        </div>
    )
}
