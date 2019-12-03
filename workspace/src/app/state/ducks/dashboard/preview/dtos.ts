export interface IPreviewState {
    searchResults: [];
    isLoading: boolean;
    error: any;
}

export function initialPreviewState(props: Partial<IPreviewState> = {}): IPreviewState {
    return {
        searchResults: [],
        isLoading: false,
        error: {},
        ...props
    }
}
