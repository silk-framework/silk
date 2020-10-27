import React from "react";
import { Autocomplete, IAutocompleteProps } from "../../../../src/app/views/shared/Autocomplete/Autocomplete";
import { changeValue, findSingleElement, logRequests, logWrapperHtml, testWrapper, withMount } from "../../TestHelper";
import { waitFor } from "@testing-library/react";

describe("AutoComplete", () => {
    const wrapper = (props: IAutocompleteProps) => {
        return withMount(testWrapper(<Autocomplete {...props} />));
    };

    it("should send exactly one request after query change", async () => {
        let counter = 0;
        const onSearch = () => {
            counter += 1;
            return [];
        };
        const autoCompletion = {
            allowOnlyAutoCompletedValues: true,
            autoCompleteValueWithLabels: false,
            autoCompletionDependsOnParameters: ["val"],
        };
        const autoComplete = wrapper({ autoCompletion, onSearch });
        expect(counter).toBe(0);
        const inputField = findSingleElement(autoComplete, "input");
        changeValue(inputField, "query");
        await waitFor(() => {
            expect(counter).toBe(1);
        });
    });
});
