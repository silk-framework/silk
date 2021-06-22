import { ISideBarState } from "../typings";

export function initialSidebarState(): ISideBarState {
    return {
        results: [],
        loading: false,
    };
}
