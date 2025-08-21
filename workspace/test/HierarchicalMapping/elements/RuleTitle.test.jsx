import React from "react";
import RuleTitle from "../../../src/app/views/pages/MappingEditor/HierarchicalMapping/elements/RuleTitle";
import { NotAvailable } from "gui-elements-deprecated";
import { ThingName } from "../../../src/app/views/pages/MappingEditor/HierarchicalMapping/components/ThingName";
import { render } from "@testing-library/react";
import { findAllDOMElements, findElement } from "../../integration/TestHelper";

const getWrapper = (renderer = render, props = {}) => renderer(<RuleTitle {...props} />);

describe("RuleTitle Component", () => {
    describe("on components mounted ", () => {
        it("should return label, when `metadata.label` presented", () => {
            const wrapper = getWrapper(render, {
                rule: {
                    metadata: {
                        label: "Something",
                    },
                },
            });
            expect(findElement(wrapper, "span").textContent).toEqual("Something");
        });

        describe("should return <ThingName />", () => {
            let wrapper;

            const rule = {
                type: "object",
                mappingTarget: {
                    uri: "ANOTHER_URI",
                },
            };

            beforeEach(() => {
                wrapper = getWrapper(render, {
                    rule,
                });
            });

            it("when `rule.type` equal to `root` and uri presented", () => {
                const wrapper = getWrapper(render, {
                    rule: {
                        type: "root",
                        rules: {
                            typeRules: [
                                {
                                    typeUri: "simple_uri",
                                },
                            ],
                        },
                    },
                });
                findElement(wrapper, "span.ecc-silk-mapping__thingname");
            });

            it("when `rule.type` equal to `object` and uri presented", () => {
                findElement(wrapper, "span.ecc-silk-mapping__thingname");
            });

            it("when `rule.type` equal to `direct` and uri presented", () => {
                wrapper = getWrapper(render, {
                    rule: {
                        ...rule,
                        type: "direct",
                    },
                });
                findElement(wrapper, "span.ecc-silk-mapping__thingname");
            });

            it("when `rule.type` equal to `complex` and uri presented", () => {
                wrapper = getWrapper(render, {
                    rule: {
                        ...rule,
                        type: "complex",
                    },
                });
                findElement(wrapper, "span.ecc-silk-mapping__thingname");
            });

            afterEach(() => {
                wrapper.unmount();
            });
        });

        describe("should return <NotAvailable />", () => {
            it("when `type` property missing in rule arg", () => {
                const wrapper = getWrapper(render, {
                    rule: {},
                });
                expect(findAllDOMElements(wrapper, "[class*='__notavailable']").length).toBeGreaterThan(0);
            });

            it("when `rule.type` equal to `root` and uri NOT presented", () => {
                const wrapper = getWrapper(render, {
                    rule: {
                        type: "root",
                        rules: {
                            typeRules: [
                                {
                                    typeUri: "",
                                },
                            ],
                        },
                    },
                });
                expect(findElement(wrapper, "span").textContent).toEqual("Mapping");
            });

            describe("when uri not presented ", () => {
                let wrapper;

                const rule = {
                    type: "object",
                    mappingTarget: {
                        uri: "",
                    },
                };

                beforeEach(() => {
                    wrapper = getWrapper(render, {
                        rule,
                    });
                });

                it("and `rule.type` equal to `object` ", () => {
                    expect(findAllDOMElements(wrapper, "[class*='__notavailable']").length).toBeGreaterThan(0);
                });

                it("and `rule.type` equal to `direct`", () => {
                    wrapper = getWrapper(render, {
                        rule: {
                            ...rule,
                            type: "direct",
                        },
                    });
                    expect(findAllDOMElements(wrapper, "[class*='__notavailable']").length).toBeGreaterThan(0);
                });

                it("and `rule.type` equal to `complex` ", () => {
                    wrapper = getWrapper(render, {
                        rule: {
                            ...rule,
                            type: "complex",
                        },
                    });
                    expect(findAllDOMElements(wrapper, "[class*='__notavailable']").length).toBeGreaterThan(0);
                });

                afterEach(() => {
                    wrapper.unmount();
                });
            });
        });
    });
});
