import React from "react";
import { SuggestField, SuggestFieldProps } from "@eccenca/gui-elements";
import { addDocumentCreateRangeMethod, findSingleElement, testWrapper, withMount } from "../../TestHelper";
import { waitFor } from "@testing-library/react";

describe("AutoComplete", () => {
    const wrapper = (props: SuggestFieldProps<any, any>) => {
        return withMount(testWrapper(<SuggestField {...props} />));
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
