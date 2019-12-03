export interface IFacetValuesState {
    count: number;
    id: string;
    label: string;
}

export interface IFacetState {
    id: string;
    label: string;
    type: string;
    values: IFacetValuesState[]
}

export interface IAppliedFacetState {
    facetId: string;
    type: string;
    keywordIds: string[];
}

export function initialAppliedFacetState(props: Partial<IAppliedFacetState> = {}): IAppliedFacetState {
    return {
        facetId: '',
        type: '',
        keywordIds: [],
        ...props
    }
}


