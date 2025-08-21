import React from "react";
import { fireEvent, render, waitFor } from "@testing-library/react";
import { NotAvailable } from "gui-elements-deprecated";
import { ThingName } from "../../../src/app/views/pages/MappingEditor/HierarchicalMapping/components/ThingName";
import RuleTypes from "../../../src/app/views/pages/MappingEditor/HierarchicalMapping/elements/RuleTypes";
import { findAllDOMElements, findElement } from "../../integration/TestHelper";

const getWrapper = (renderer = render, props = {}) => renderer(<RuleTypes {...props} />);

describe("RuleTypes Component", () => {
    describe("on components mounted ", () => {
        describe("when `rule.type` equal to `object` ", () => {
            it("and `rule.rules.typeRules` presented, should return array of <ThingName />", () => {
                const wrapper = getWrapper(render, {
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
                expect(findAllDOMElements(wrapper, "span.ecc-silk-mapping__thingname").length).toBeGreaterThan(0);
            });

            it("and `rule.rules.typeRules` Empty, should return <NotAvailable />", () => {
                const wrapper = getWrapper(render, {
                    rule: {
                        type: "object",
                        rules: {
                            typeRules: [],
                        },
                    },
                });
                expect(findAllDOMElements(wrapper, "[class*='__notavailable']").length).toBeGreaterThan(0);
            });
        });

        describe("should return text, when `rule.type` equal to `direct` or `complex` ", () => {
            it("and `rule.mappingTarget.valueType.nodeType` presented", () => {
                const wrapper = getWrapper(render, {
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

            xit("and `rule.mappingTarget.valueType.nodeType` NOT presented", () => {
                const wrapper = getWrapper(render, {
                    rule: {
                        type: "direct",
                        mappingTarget: {
                            valueType: {},
                        },
                    },
                });
                expect(findAllDOMElements(wrapper, "[class*='__notavailable']").length).toBeGreaterThan(0);
            });

            it("and language prefix presented", () => {
                const wrapper = getWrapper(render, {
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
            const wrapperNoType = getWrapper(render, {
                rule: {
                    type: "root",
                },
            });
            const notAvailable = findAllDOMElements(wrapperNoType, "[class*='__notavailable']");
            const span = findAllDOMElements(wrapperNoType, "span");
            expect(span[0].innerHTML).toContain(notAvailable[0].innerHTML);
            expect(wrapperNoType.container.innerHTML).toContain("not available");
            const wrapperWithType = getWrapper(render, {
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
