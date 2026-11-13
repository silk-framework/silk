import { filtersSlice } from "./filtersSlice";
import { initialFiltersState } from "./initialState";

describe("filtersSlice", () => {
    it("retains page query configuration when applying and resetting search filters", () => {
        const configuredState = filtersSlice.reducer(
            initialFiltersState(),
            filtersSlice.actions.setPreservedQueryKeys(["context"]),
        );
        const filteredState = filtersSlice.reducer(
            configuredState,
            filtersSlice.actions.applyFilters({ textQuery: "people" }),
        );
        const resetState = filtersSlice.reducer(filteredState, filtersSlice.actions.resetFilters());

        expect(resetState.appliedFilters.textQuery).toBe("");
        expect(resetState.preservedQueryKeys).toEqual(["context"]);
    });

    it("keeps an empty text query in the applied filter state", () => {
        const stateWithQuery = filtersSlice.reducer(
            initialFiltersState(),
            filtersSlice.actions.applyFilters({ textQuery: "people" }),
        );

        const clearedState = filtersSlice.reducer(stateWithQuery, filtersSlice.actions.applyFilters({ textQuery: "" }));

        expect(clearedState.appliedFilters.textQuery).toBe("");
        expect(clearedState.appliedFilters).toHaveProperty("textQuery");
    });
});
