import { ISearchResultsServer } from "@ducks/workspace/typings";

export interface ISideBarState {
    /***
     * The server response containing an array of tasks
     * that match the search query or recently viewed tasks
     ****/
    results: Array<ISearchResultsServer>;

    /** state that is toggled if, or not, pending requests exists based on the search query*/
    loading: boolean;
}
