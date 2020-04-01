import React, { useEffect } from "react";
import { useDispatch, useSelector } from "react-redux";
import { useParams } from "react-router";
import { workspaceOp, workspaceSel } from "@ducks/workspace";
import { globalOp, globalSel } from "@ducks/global";
import { routerSel } from "@ducks/router";
import Checkbox from "@wrappers/blueprint/checkbox";
import {
    Spacing,
    TitleSubsection,
} from "@wrappers/index";
import FacetsList from "./FacetsList";

export function Filterbar() {
    const dispatch = useDispatch();
    const {projectId} = useParams();

    const appliedFilters = useSelector(workspaceSel.appliedFiltersSelector);
    const modifiers = useSelector(globalSel.availableDTypesSelector);
    const qs = useSelector(routerSel.routerSearchSelector);

    const typeModifier = modifiers.type;

    useEffect(() => {
        // Reset the filters, due to redirecting
        dispatch(workspaceOp.resetFilters());
        // Setup the filters from query string
        dispatch(workspaceOp.setupFiltersFromQs(qs));
        dispatch(globalOp.fetchAvailableDTypesAsync(projectId));
    }, []);

    const handleFilterSelect = (field: string, val: string) => {
        let value = val !== appliedFilters[field] ? val : '';
        dispatch(workspaceOp.applyFiltersOp({
            [field]: value
        }));
    };

    return (
        <nav>
            {
                typeModifier &&
                <>
                    <TitleSubsection>{typeModifier.label}</TitleSubsection>
                    <Spacing size="tiny" />
                    <ul>
                    {
                        typeModifier.options.map(opt =>
                            <li key={opt.id}><Checkbox
                                checked={appliedFilters[typeModifier.field] === opt.id}
                                label={opt.label}
                                onChange={() => handleFilterSelect(typeModifier.field, opt.id)}
                                value={opt.id}
                            /></li>
                        )
                    }
                    </ul>
                    <Spacing />
                    <FacetsList/>
                </>
            }
        </nav>
    )
}
