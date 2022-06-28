export interface IPagination {
    page: number;

    pageSize: number;
}

export default function paginate<T>(arr: T[], pagination: IPagination): T[] {
    const {page, pageSize} = pagination;
    const startIndex = (page - 1) * pageSize;

    return arr.slice(startIndex, startIndex + pageSize);
}
