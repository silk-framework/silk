import ruleBlockUtils from "../ruleBlock.utils";
import { IRuleOperatorNode } from "../../../shared/RuleEditor/RuleEditor.typings";
import { IRuleBlockPort } from "../ruleBlock.types";

describe("ruleBlockUtils", () => {
    it("should resolve the logical port ID from the node parameter value", () => {
        expect(
            ruleBlockUtils.resolvePortId({
                ...inputPortNode("portNode1", "resolvedPort", 1),
                parameters: {
                    ...inputPortNode("portNode1", "resolvedPort", 1).parameters,
                    portId: "  resolvedPort  ",
                },
            }),
        ).toBe("resolvedPort");
    });

    it("should return undefined when the logical port ID parameter is missing or blank", () => {
        expect(
            ruleBlockUtils.resolvePortId({
                ...inputPortNode("portNode1", "ignoredPort", 1),
                parameters: {
                    ...inputPortNode("portNode1", "ignoredPort", 1).parameters,
                    portId: "   ",
                },
            }),
        ).toBeUndefined();
        expect(
            ruleBlockUtils.resolvePortId({
                ...inputPortNode("portNode2", "ignoredPort", 1),
                parameters: {
                    ...inputPortNode("portNode2", "ignoredPort", 1).parameters,
                    portId: undefined,
                },
            }),
        ).toBeUndefined();
    });

    it("should throw when a logical port ID is required but missing", () => {
        expect(() =>
            ruleBlockUtils.requirePortId({
                ...inputPortNode("portNode1", "ignoredPort", 1),
                parameters: {
                    ...inputPortNode("portNode1", "ignoredPort", 1).parameters,
                    portId: "   ",
                },
            }),
        ).toThrow("InputPortOperator node 'portNode1' is missing parameters.portId.");
    });

    it("should reject logical input-port states with missing or duplicate IDs", () => {
        expect(() =>
            ruleBlockUtils.assertValidPorts([
                {
                    ...port("firstPort", 1, "First port"),
                    id: "   ",
                },
            ]),
        ).toThrow("Rule block input ports must have a non-empty ID.");
        expect(() =>
            ruleBlockUtils.assertValidPorts([port("sharedPort", 1, "First port"), port("sharedPort", 2, "Second port")]),
        ).toThrow("Rule block input port IDs must be unique. Duplicate ID 'sharedPort' found.");
    });

    it("should sort rule block ports by display order and then by ID as deterministic fallback for duplicated orders", () => {
        // Valid editor state keeps display orders unique. The secondary ID sort is only needed for transiently
        // duplicated orders while validation is still running.
        expect(
            ruleBlockUtils.sortRuleBlockPorts([
                port("thirdPort", 2, "Third port"),
                port("firstPort", 1, "First port"),
                port("secondPort", 2, "Second port"),
            ]),
        ).toStrictEqual([
            port("firstPort", 1, "First port"),
            port("secondPort", 2, "Second port"),
            port("thirdPort", 2, "Third port"),
        ]);
    });

    it("should return the next input-port defaults based on the current max display order", () => {
        expect(
            ruleBlockUtils.nextInputPortDefaults([
                port("firstPort", 2, "First port"),
                port("secondPort", 5, "Second port"),
            ]),
        ).toStrictEqual({
            label: "Input 6",
            description: "",
            exampleValues: "",
            displayOrder: 6,
            deprecated: false,
        });
    });

    it("should return the ports whose visible display order changed", () => {
        expect(
            ruleBlockUtils.portsWithChangedDisplayOrder(
                [port("firstPort", 1, "First port"), port("secondPort", 2, "Second port")],
                [
                    port("firstPort", 1, "First port"),
                    port("secondPort", 3, "Second port"),
                    port("thirdPort", 4, "Third port"),
                ],
            ),
        ).toStrictEqual([
            port("secondPort", 3, "Second port"),
            port("thirdPort", 4, "Third port"),
        ]);
    });

    it("should reject duplicate display orders across different rule block ports", () => {
        const collectionResult = ruleBlockUtils.collectPortDefinitions(
            [],
            [
                inputPortNode("portNode1", "firstPort", 1),
                inputPortNode("portNode2", "secondPort", 1),
            ],
            "invalid display order",
            (portId) => `conflicting definition for ${portId}`,
        );

        expect(collectionResult.nodeErrors).toStrictEqual([]);
        expect(collectionResult.portDefinitions).toStrictEqual([
            port("firstPort", 1, "firstPort label"),
            port("secondPort", 1, "secondPort label"),
        ]);
        expect(
            ruleBlockUtils.validateDuplicateDisplayOrders(
                collectionResult.portDefinitions ?? [],
                [
                    inputPortNode("portNode1", "firstPort", 1),
                    inputPortNode("portNode2", "secondPort", 1),
                ],
                (displayOrder) => `duplicate display order ${displayOrder}`,
            ),
        ).toStrictEqual([
            {
                nodeId: "portNode1",
                message: "duplicate display order 1",
            },
            {
                nodeId: "portNode2",
                message: "duplicate display order 1",
            },
        ]);
    });

    it("should report malformed input-port nodes that are missing their logical port ID", () => {
        expect(
            ruleBlockUtils.validateMissingPortIds(
                [
                    {
                        ...inputPortNode("portNode1", "ignoredPort", 1),
                        parameters: {
                            ...inputPortNode("portNode1", "ignoredPort", 1).parameters,
                            portId: "",
                        },
                    },
                    inputPortNode("portNode2", "validPort", 2),
                ],
                () => "missing port ID",
            ),
        ).toStrictEqual([
            {
                nodeId: "portNode1",
                message: "missing port ID",
            },
        ]);
    });

    it("should allow reusing the same logical port in multiple places without creating duplicate port definitions", () => {
        const result = ruleBlockUtils.collectPortDefinitions(
            [],
            [
                inputPortNode("portNode1", "sharedPort", 1),
                inputPortNode("portNode2", "sharedPort", 1),
            ],
            "invalid display order",
            (portId) => `conflicting definition for ${portId}`,
        );

        expect(result.nodeErrors).toStrictEqual([]);
        expect(result.portDefinitions).toStrictEqual([
            {
                id: "sharedPort",
                label: "Shared",
                description: "",
                exampleValues: "",
                displayOrder: 1,
                deprecated: false,
            },
        ]);
    });

    it("should reject removing a persisted port when the rule block is already in use", () => {
        const result = ruleBlockUtils.validateUsedPortCompatibility(
            [port("lockedPort", 0, "Locked port")],
            [],
            [],
            (portName) => `removed ${portName}`,
            (portName) => `reordered ${portName}`,
        );

        expect(result).toStrictEqual({
            errorMessage: "removed Locked port",
            nodeErrors: [],
        });
    });

    it("should reject changing the relative order of persisted ports when the rule block is already in use", () => {
        const result = ruleBlockUtils.validateUsedPortCompatibility(
            [port("firstPort", 0, "First port"), port("secondPort", 2, "Second port")],
            [port("firstPort", 4, "First port"), port("secondPort", 1, "Second port")],
            [inputPortNode("portNode1", "firstPort", 4), inputPortNode("portNode2", "secondPort", 1)],
            (portName) => `removed ${portName}`,
            (portName) => `reordered ${portName}`,
        );

        expect(result).toStrictEqual({
            errorMessage: "reordered First port reordered Second port",
            nodeErrors: [
                {
                    nodeId: "portNode1",
                    message: "reordered First port",
                },
                {
                    nodeId: "portNode2",
                    message: "reordered Second port",
                },
            ],
        });
    });

    it("should allow changing absolute display order numbers if the relative order stays the same", () => {
        const result = ruleBlockUtils.validateUsedPortCompatibility(
            [port("firstPort", 2, "First port"), port("secondPort", 5, "Second port")],
            [port("firstPort", 1, "First port"), port("secondPort", 2, "Second port")],
            [inputPortNode("portNode1", "firstPort", 1), inputPortNode("portNode2", "secondPort", 2)],
            (portName) => `removed ${portName}`,
            (portName) => `reordered ${portName}`,
        );

        expect(result).toStrictEqual({
            errorMessage: undefined,
            nodeErrors: [],
        });
    });

    it("should allow metadata changes on persisted ports when the rule block is already in use", () => {
        const result = ruleBlockUtils.validateUsedPortCompatibility(
            [port("lockedPort", 0, "Locked port")],
            [
                {
                    ...port("lockedPort", 0, "Locked port"),
                    label: "Updated label",
                    description: "Updated description",
                    exampleValues: "- updated",
                    deprecated: true,
                },
            ],
            [inputPortNode("portNode1", "lockedPort", 0)],
            (portName) => `removed ${portName}`,
            (portName) => `reordered ${portName}`,
        );

        expect(result).toStrictEqual({
            errorMessage: undefined,
            nodeErrors: [],
        });
    });

    it("should fall back to the port ID in compatibility errors if the label is empty", () => {
        const result = ruleBlockUtils.validateUsedPortCompatibility(
            [port("lockedPort", 0, "")],
            [],
            [],
            (portName) => `removed ${portName}`,
            (portName) => `reordered ${portName}`,
        );

        expect(result).toStrictEqual({
            errorMessage: "removed lockedPort",
            nodeErrors: [],
        });
    });

    it("should allow collecting temporarily duplicated display orders so used-port compatibility can be validated first", () => {
        const result = ruleBlockUtils.collectPortDefinitions(
            [port("hiddenPort", 1, "Hidden port"), port("lockedPort", 2, "Locked port")],
            [inputPortNode("portNode1", "lockedPort", 1)],
            "invalid display order",
            (portId) => `conflicting definition for ${portId}`,
        );

        expect(result).toStrictEqual({
            nodeErrors: [],
            portDefinitions: [port("hiddenPort", 1, "Hidden port"), port("lockedPort", 1, "lockedPort label")],
        });
    });

    it("should normalize display orders to dense ranks while preserving the current relative order", () => {
        expect(
            ruleBlockUtils.normalizePortDisplayOrder([
                port("secondPort", 5, "Second port"),
                port("firstPort", 2, "First port"),
                port("thirdPort", 8, "Third port"),
            ]),
        ).toStrictEqual([
            port("firstPort", 1, "First port"),
            port("secondPort", 2, "Second port"),
            port("thirdPort", 3, "Third port"),
        ]);
    });

    it("should detect whether display orders are already normalized", () => {
        expect(
            ruleBlockUtils.isNormalizedPortDisplayOrder([
                port("firstPort", 1, "First port"),
                port("secondPort", 2, "Second port"),
            ]),
        ).toBe(true);
        expect(
            ruleBlockUtils.isNormalizedPortDisplayOrder([
                port("firstPort", 2, "First port"),
                port("secondPort", 5, "Second port"),
            ]),
        ).toBe(false);
    });
});

const inputPortNode = (nodeId: string, portId: string, displayOrder: number): IRuleOperatorNode => ({
    nodeId,
    pluginType: "InputPortOperator",
    pluginId: "inputPort",
    label: "Input port",
    parameters: {
        portId,
        label: portId === "sharedPort" ? "Shared" : `${portId} label`,
        description: "",
        exampleValues: "",
        displayOrder: String(displayOrder),
        deprecated: "false",
    },
    portSpecification: {
        type: "count",
        minInputPorts: 0,
        maxInputPorts: 0,
    },
    inputs: [],
    inputsCanBeSwitched: false,
    tags: [],
});

const port = (id: string, displayOrder: number, label: string = id): IRuleBlockPort => ({
    id,
    label,
    description: "",
    exampleValues: "",
    displayOrder,
    deprecated: false,
});
