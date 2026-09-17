import { filtersSlice } from "./filtersSlice";
import { initialFiltersState } from "./initialState";

describe("filtersSlice", () => {
    it("keeps an empty text query in the applied filter state", () => {
        const stateWithQuery = filtersSlice.reducer(
            initialFiltersState(),
            filtersSlice.actions.applyFilters({ textQuery: "people" }),
        );

        const clearedState = filtersSlice.reducer(
            stateWithQuery,
            filtersSlice.actions.applyFilters({ textQuery: "" }),
        );

        expect(clearedState.appliedFilters.textQuery).toBe("");
        expect(clearedState.appliedFilters).toHaveProperty("textQuery");
    });
});
