import React from "react";
import AutoComplete from "../../../src/app/views/pages/MappingEditor/HierarchicalMapping/components/AutoComplete";
import { render } from "@testing-library/react";
import { findElement } from "../../integration/TestHelper";

const props = {
    input: "text",
    ruleId: "rule",
};
const autocompleteAsyncMock = jest.fn();
jest.doMock("../../../src/app/views/pages/MappingEditor/HierarchicalMapping/store", () => autocompleteAsyncMock);

const getWrapper = () => render(<AutoComplete {...props} />);

describe("AutoComplete Component", () => {
    describe("on component mounted, ", () => {
        let wrapper;
        beforeEach(() => {
            wrapper = getWrapper();
        });

        it("should render AutoCompleteBox component", () => {
            findElement(wrapper, "[class*='-suggestfield']");
        });

        afterEach(() => {
            wrapper.unmount();
        });
    });
});
