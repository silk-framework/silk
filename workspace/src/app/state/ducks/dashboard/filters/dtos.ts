import { PaginationDto } from "../../../dto";

export class Modifier {
    label: string;
    field: string;
    options: any[] = [];
}

export class Modifiers {
    [key: string]: Modifier
}

export class AppliedFilters {
    textQuery: string;
    sort: string;
    sortOrder: string;
}

export class FiltersDto {
    facets: any[] = [];
    modifiers: Modifiers = new Modifiers();
    appliedFilters: AppliedFilters = new AppliedFilters();
    pagination: PaginationDto = new PaginationDto();
}
