import React from "react";
import "@testing-library/jest-dom";

import MappingsList from "../../../../src/app/views/pages/MappingEditor/HierarchicalMapping/containers/MappingsList/MappingsList";
import { findAllDOMElements, findElement, renderWrapper } from "../../../integration/TestHelper";
import { RenderResult } from "@testing-library/react";

const props = {
    rules: [
        {
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
        },
        {
            type: "complex",
            id: "1",
            operator: {
                type: "transformInput",
                id: "buildUri",
                function: "concat",
                inputs: [
                    {
                        type: "transformInput",
                        id: "constant0",
                        function: "constant",
                        inputs: [],
                        parameters: {
                            value: "",
                        },
                    },
                    {
                        type: "transformInput",
                        id: "fixUri1",
                        function: "uriFix",
                        inputs: [
                            {
                                type: "pathInput",
                                id: "path1",
                                path: "city",
                            },
                        ],
                        parameters: {
                            uriPrefix: "urn:url-encoded-value:",
                        },
                    },
                    {
                        type: "transformInput",
                        id: "constant2",
                        function: "constant",
                        inputs: [],
                        parameters: {
                            value: "/1",
                        },
                    },
                ],
                parameters: {
                    glue: "",
                    missingValuesAsEmptyStrings: "false",
                },
            },
            sourcePaths: ["city"],
            metadata: {
                description: "33",
                label: "2",
            },
            mappingTarget: {
                uri: "1",
                valueType: {
                    nodeType: "UriValueType",
                },
                isBackwardProperty: false,
                isAttribute: false,
            },
        },
    ],
    currentRuleId: "root",
    loading: false,
};

const getWrapper = (args: any = props): RenderResult => {
    return renderWrapper(<MappingsList {...args} />);
};

const droppableSelector = `[data-rbd-droppable-id="droppable"]`;

describe("MappingsList Component", () => {
    describe("on component mounted, ", () => {
        let wrapper;
        beforeEach(() => {
            wrapper = getWrapper();
        });

        it("should render EmptyList component, when rules array is empty", () => {
            const wrapper = getWrapper({
                ...props,
                rules: [],
            });
            expect(wrapper.container.querySelector(droppableSelector)).not.toBeInTheDocument();
        });

        it("should render DragDropContext component, when rules is NOT empty", () => {
            expect(findElement(wrapper, droppableSelector)).toBeInTheDocument();
        });

        it("should render DraggableItem component, the right count", () => {
            expect(findAllDOMElements(wrapper, `${droppableSelector}  li`)).toHaveLength(2);
        });

        it("should render ListActions component", () => {
            expect(wrapper.getByText("Add value mapping")).toBeInTheDocument();
        });

        afterEach(() => {
            wrapper.unmount();
        });
    });
});
