import qs from "qs";
import { IAppliedFacetState, IAppliedFiltersState } from "@ducks/workspace/typings";

const values = (value: unknown): string[] => {
    if (typeof value === "string") {
        return value.split(",");
    }
    return Array.isArray(value) ? value.filter((item): item is string => typeof item === "string") : [];
};

/** Ignores navigation context when comparing URL filters with the search store. */
export const changeSearchMatches = (
    search: string,
    filters: IAppliedFiltersState,
    facets: readonly IAppliedFacetState[],
    page: number,
): boolean => {
    const parsed = qs.parse(search, { comma: true, ignoreQueryPrefix: true });
    const ids = values(parsed.f_ids);
    const types = values(parsed.types);
    const keywords = values(parsed.f_keys);
    const fromUrl = {
        textQuery: typeof parsed.textQuery === "string" ? parsed.textQuery : "",
        itemType: typeof parsed.itemType === "string" ? parsed.itemType : "",
        page: Number(parsed.page) || 1,
        facets: ids.map((facetId, index) => ({
            facetId,
            type: types[index] ?? "",
            keywordIds: (keywords[index] ?? "").split("|"),
        })),
    };
    return (
        JSON.stringify(fromUrl) ===
        JSON.stringify({
            textQuery: filters.textQuery || "",
            itemType: filters.itemType || "",
            page,
            facets: facets.map(({ facetId, type, keywordIds }) => ({ facetId, type: type ?? "", keywordIds })),
        })
    );
};
