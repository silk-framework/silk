import { useDispatch } from "react-redux";
import { routerOp } from "@ducks/router";
import { IPageLabels } from "@ducks/router/operations";
import { ISearchResultsServer } from "@ducks/workspace/typings";
import { DATA_TYPES } from "../../../constants";
import { AppDispatch } from "store/configureStore";

/**
 * Navigation helpers shared by the different presentations of a search result item
 * (row card, table row, grid card). Provides the item links and a click handler that
 * opens the item's details page while still allowing "open in new tab" (CTRL/CMD click).
 */
export const useItemNavigation = (item: ISearchResultsServer) => {
    const dispatch = useDispatch<AppDispatch>();
    const itemLinks = item.itemLinks ?? [];
    const detailsPath = itemLinks.length ? itemLinks[0].path : "";

    const goToDetailsPage = (e: React.MouseEvent) => {
        // Only open page in same tab if user did not try to open in new tab (CTRL or CMD click)
        if (!e?.ctrlKey && !e?.metaKey && itemLinks.length > 0) {
            e.preventDefault();
            const labels: IPageLabels = Object.create(null);
            if (item.type === DATA_TYPES.PROJECT) {
                labels.projectLabel = item.label;
            } else {
                labels.taskLabel = item.label;
            }
            labels.itemType = item.type;
            dispatch(routerOp.goToPage(detailsPath, labels));
        }
    };

    return { itemLinks, detailsPath, goToDetailsPage };
};
