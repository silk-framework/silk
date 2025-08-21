import React from "react";
import RootMappingRule from "../../../src/app/views/pages/MappingEditor/HierarchicalMapping/containers/RootMappingRule";
import { fireEvent, render } from "@testing-library/react";
import { findAllDOMElements, findElement, byTestId } from "../../integration/TestHelper";

const props = {
    rule: {
        type: "complex",
        id: "country",
        operator: {
            type: "transformInput",
            id: "normalize",
            function: "GeoLocationParser",
            inputs: [
                {
                    type: "pathInput",
                    id: "country",
                    path: "country",
                },
            ],
            parameters: {
                parseTypeId: "Country",
                fullStateName: "true",
            },
        },
        sourcePaths: ["country"],
        metadata: {
            description: "sss",
            label: "",
        },
        mappingTarget: {
            uri: "<urn:ruleProperty:country>",
            valueType: {
                nodeType: "UriValueType",
            },
            isBackwardProperty: false,
            isAttribute: false,
        },
        rules: {
            uriRule: {
                pattern: "pattern",
            },
        },
    },
    parentRuleId: "root",
};

const selectors = {
    URI_PATTERN: ".ecc-silk-mapping__rulesobject__title-uripattern",
};

const getWrapper = (renderer = render, args = props) => renderer(<RootMappingRule {...args} />);

describe("RootMappingRule Component", () => {
    describe("on component mounted, ", () => {
        let wrapper;
        beforeEach(() => {
            wrapper = getWrapper(render);
        });

        it("should return null, when rule is empty", () => {
            const wrapper = getWrapper(render, {
                ...props,
                rule: {},
            });
            expect(wrapper.container.childNodes.length).toBeFalsy();
        });

        it("should NotAvailable rendered in uriPattern, when rule type is not Uri or Complex", () => {
            const wrapper = getWrapper(render, {
                ...props,
                rule: {
                    ...props.rule,
                    type: "root",
                },
            });
            expect(findAllDOMElements(wrapper, "[class*='__notavailable']").length).toBeGreaterThan(0);
        });

        it("should uriPattern is equal to `rules.uriRule.pattern`, when uriRule type is uri", () => {
            const wrapper = getWrapper(render, {
                ...props,
                rule: {
                    ...props.rule,
                    rules: {
                        ...props.rule.rules,
                        uriRule: {
                            type: "uri",
                            pattern: "pattern",
                        },
                    },
                },
            });

            expect(findElement(wrapper, selectors.URI_PATTERN).textContent).toBe("pattern");
        });

        it("should uriPattern is equal to `URI formula`, when uriRule type is complex", () => {
            const wrapper = getWrapper(render, {
                ...props,
                rule: {
                    ...props.rule,
                    rules: {
                        ...props.rule.rules,
                        uriRule: {
                            type: "complexUri",
                        },
                    },
                },
            });
            expect(findElement(wrapper, selectors.URI_PATTERN).textContent).toBe("URI formula");
        });

        it("should render the ObjectRule component, when rule is expanded", () => {
            window.HTMLElement.prototype.scrollIntoView = jest.fn();
            fireEvent.click(
                wrapper.container.querySelector(`${byTestId("root-mapping-rule")} [class*='card__header']`),
            );
            expect(findElement(wrapper, ".ecc-silk-mapping__rulesviewer")).toBeInTheDocument();
        });

        afterEach(() => {
            wrapper.unmount();
        });
    });
});
