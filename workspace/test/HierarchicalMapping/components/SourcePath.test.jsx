import { shallow } from "enzyme";
import { NotAvailable } from "gui-elements-deprecated";
import React from "react";

import { SourcePath } from "../../../src/app/views/pages/MappingEditor/HierarchicalMapping/components/SourcePath";

const getWrapper = (renderer = shallow, args = {}) => renderer(<SourcePath {...args} />);

describe("SourcePath Component", () => {
    describe("on component mounted, ", () => {
        it("should render NotAvailable component, when `sourcePath` is NOT presented in `rule` prop", () => {
            const wrapper = getWrapper(shallow, {
                rule: {},
            });
            expect(wrapper.find(NotAvailable)).toHaveLength(1);
        });

        it("should render NotAvailable component, when `sourcePath` is presented in `rule` prop", () => {
            const wrapper = getWrapper(shallow, {
                rule: {
                    sourcePath: "text",
                },
            });
            expect(wrapper.find("span").text()).toEqual("text");
        });
    });
});
