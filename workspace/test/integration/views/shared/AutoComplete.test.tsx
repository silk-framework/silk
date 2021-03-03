import React from "react";
import { Autocomplete, IAutocompleteProps } from "../../../../src/app/views/shared/Autocomplete/Autocomplete";
import { addDocumentCreateRangeMethod, changeValue, findSingleElement, testWrapper, withMount } from "../../TestHelper";
import { waitFor } from "@testing-library/react";

describe("AutoComplete", () => {
    const wrapper = (props: IAutocompleteProps<any, any>) => {
        return withMount(testWrapper(<Autocomplete {...props} />));
    };

    it("should send exactly one request when receiving focus", async () => {
        // document.createRange is needed from the popover of the auto-complete element
        addDocumentCreateRangeMethod();
        let counter = 0;
        const onSearch = () => {
            counter += 1;
            return [];
        };
        const autoCompletion = {
            allowOnlyAutoCompletedValues: true,
            autoCompleteValueWithLabels: false,
            autoCompletionDependsOnParameters: [],
        };
        const autoComplete = wrapper({ autoCompletion, onSearch });
        expect(counter).toBe(0);
        const inputField = findSingleElement(autoComplete, "input");
        inputField.simulate("focus");
        await waitFor(() => {
            expect(counter).toBe(1);
        });
    });
});
