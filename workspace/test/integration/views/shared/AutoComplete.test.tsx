import React from "react";
import { SuggestField, SuggestFieldProps } from "@eccenca/gui-elements";
import { findElement, renderWrapper } from "../../TestHelper";
import { act, waitFor } from "@testing-library/react";

describe("AutoComplete", () => {
    const wrapper = (props: SuggestFieldProps<any, any>) => {
        return renderWrapper(<SuggestField {...props} />);
    };

    it("should send exactly one request when receiving focus", async () => {
        let counter = 0;
        const onSearch = () => {
            counter += 1;
            return [];
        };
        const autoComplete = wrapper({ onSearch });
        expect(counter).toBe(0);
        const inputField = findElement(autoComplete, "input");
        act(() => {
            inputField.focus();
        });
        await waitFor(() => {
            expect(counter).toBe(1);
        });
    });
});
