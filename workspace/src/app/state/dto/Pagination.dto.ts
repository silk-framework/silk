export interface IPaginationState {
    limit: number;
    offset: number;
    current: number;
    total: number;
}

export function initialPaginationState(props: Partial<IPaginationState> = {}): IPaginationState {
    return {
        limit: 5,
        offset: 0,
        current: 1,
        total: 0,
        ...props
    }
}
