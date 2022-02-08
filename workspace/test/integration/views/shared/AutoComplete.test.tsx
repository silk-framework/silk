import React from "react";
import {
    AutoCompleteField,
    IAutoCompleteFieldProps,
} from "gui-elements/src/components/AutocompleteField/AutoCompleteField";
import { addDocumentCreateRangeMethod, findSingleElement, testWrapper, withMount } from "../../TestHelper";
import { waitFor } from "@testing-library/react";

describe("AutoComplete", () => {
    const wrapper = (props: IAutoCompleteFieldProps<any, any>) => {
        return withMount(testWrapper(<AutoCompleteField {...props} />));
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
        const autoComplete = wrapper({ onSearch });
        expect(counter).toBe(0);
        const inputField = findSingleElement(autoComplete, "input");
        inputField.simulate("focus");
        await waitFor(() => {
            expect(counter).toBe(1);
        });
    });
});
