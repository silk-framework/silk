import React from "react";
import { URIInfo } from "../../../src/app/views/pages/MappingEditor/HierarchicalMapping/components/URIInfo";
import { fireEvent, render } from "@testing-library/react";
import {findAllDOMElements, findElement, logRequests} from "../../integration/TestHelper";

const getVocabInfoAsyncMock = jest.fn();

jest.doMock("../../../src/app/views/pages/MappingEditor/HierarchicalMapping/store", () => getVocabInfoAsyncMock);
const props = {
    uri: "<superUri>",
    fallback: "Fallback text",
    field: "field",
};

const getWrapper = (renderer = render, args = props) => renderer(<URIInfo {...args} />);

describe("URIInfo Component", () => {
    describe("on component mounted, ", () => {
        it("should render fallback text, when text is NOT available from server", () => {
            const wrapper = getWrapper(render);
            expect(findElement(wrapper, "span").textContent).toEqual("Fallback text");
        });

        it("should render NotAvailable component, when fallback not available and `uri` in not string", () => {
            const wrapper = getWrapper(render, {
                ...props,
                uri: {},
                fallback: undefined,
            });
            expect(findAllDOMElements(wrapper, "[class*='__notavailable']").length).toBeGreaterThan(0);
        });

        it("should render text from `uri`, when `props.field` equal to 'label'", () => {
            const wrapper = getWrapper(render, {
                ...props,
                field: "label",
                fallback: undefined,
            });
            expect(findElement(wrapper, "span").textContent).toEqual("superUri");
        });
    });
});
