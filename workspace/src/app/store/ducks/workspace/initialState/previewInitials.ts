import { IPreviewState } from "@ducks/workspace/typings";

export function initialPreviewState(props: Partial<IPreviewState> = {}): IPreviewState {
    return {
        searchResults: [],
        isLoading: false,
        error: {},
        ...props
    }
}
