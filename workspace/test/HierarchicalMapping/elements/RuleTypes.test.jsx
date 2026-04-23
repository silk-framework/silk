import React from "react";
import { render } from "@testing-library/react";
import RuleTypes from "../../../src/app/views/pages/MappingEditor/HierarchicalMapping/elements/RuleTypes";
import {checkForNotAvailableElement, findAllDOMElements, findElement} from "../../integration/TestHelper";

const getWrapper = (props = {}) => render(<RuleTypes {...props} />);

describe("RuleTypes Component", () => {
    describe("on components mounted ", () => {
        describe("when `rule.type` equal to `object` ", () => {
            it("and `rule.rules.typeRules` presented, should return array of <ThingName />", () => {
                const wrapper = getWrapper({
                    rule: {
                        type: "object",
                        rules: {
                            typeRules: [
                                {
                                    typeUri: "simple_uri",
                                },
                                {
                                    typeUri: "simple_uri1",
                                },
                            ],
                        },
                    },
                });
                expect(findAllDOMElements(wrapper, "span.ecc-silk-mapping__uriinfo").length).toBeGreaterThan(0);
            });

            it("and `rule.rules.typeRules` Empty, should return <NotAvailable />", () => {
                const wrapper = getWrapper({
                    rule: {
                        type: "object",
                        rules: {
                            typeRules: [],
                        },
                    },
                });
                checkForNotAvailableElement(wrapper)
            });
        });

        describe("should return text, when `rule.type` equal to `direct` or `complex` ", () => {
            it("and `rule.mappingTarget.valueType.nodeType` presented", () => {
                const wrapper = getWrapper({
                    rule: {
                        type: "direct",
                        mappingTarget: {
                            valueType: {
                                nodeType: "dummy",
                            },
                        },
                    },
                });
                expect(findElement(wrapper, "span").textContent).toEqual("dummy");
            });

            it("and language prefix presented", () => {
                const wrapper = getWrapper({
                    rule: {
                        type: "complex",
                        mappingTarget: {
                            valueType: {
                                nodeType: "dummy",
                                lang: "prefix",
                            },
                        },
                    },
                });
                expect(findElement(wrapper, "span").textContent).toEqual("dummy (prefix)");
            });
        });

        it("should return proper types for root mapping", () => {
            const wrapperNoType = getWrapper({
                rule: {
                    type: "root",
                },
            });
            checkForNotAvailableElement(wrapperNoType)
            const wrapperWithType = getWrapper({
                rule: {
                    type: "root",
                    rules: {
                        typeRules: [
                            {
                                typeUri: "<https://domain.com/expectedType>",
                            },
                        ],
                    },
                },
            });
            expect(wrapperWithType.container.innerHTML).toContain(">expectedType<");
        });
    });
});
