import React from "react";

import { routerOp } from "@ducks/router";
import { IPageLabels } from "@ducks/router/operations";
import { workspaceOp, workspaceSel } from "@ducks/workspace";
import { DATA_TYPES } from "../constants";
import { batch, useDispatch, useSelector } from "react-redux";

export const useSelectFirstResult = () => {
    const dispatch = useDispatch();
    const data = useSelector(workspaceSel.resultsSelector);
    const dataArrayRef = React.useRef(data);

    React.useEffect(() => {
        dataArrayRef.current = data;
    }, [data]);

    const onEnter = React.useCallback(() => {
        const firstResult = dataArrayRef.current[0];
        const labels: IPageLabels = {};
        if (firstResult && firstResult.itemLinks?.length) {
            if (firstResult.type === DATA_TYPES.PROJECT) {
                labels.projectLabel = firstResult.label;
            } else {
                labels.taskLabel = firstResult.label;
            }
            labels.itemType = firstResult.type;
            batch(() => {
                dispatch(workspaceOp.applyFiltersOp({ textQuery: "" }));
                setTimeout(() => dispatch(routerOp.goToPage(firstResult.itemLinks![0].path, labels)), 0);
            });
        }
    }, []);

    return { onEnter };
};
