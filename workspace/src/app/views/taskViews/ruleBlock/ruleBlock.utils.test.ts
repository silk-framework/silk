import ruleBlockUtils from "./ruleBlock.utils";
import { IRuleOperatorNode } from "../../shared/RuleEditor/RuleEditor.typings";
import { IRuleBlockPort } from "./ruleBlock.types";

describe("ruleBlockUtils", () => {
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

    it("should reject reordering a persisted port when the rule block is already in use", () => {
        const result = ruleBlockUtils.validateUsedPortCompatibility(
            [port("lockedPort", 0, "Locked port")],
            [port("lockedPort", 3, "Renamed port")],
            [inputPortNode("portNode1", "lockedPort", 3)],
            (portName) => `removed ${portName}`,
            (portName) => `reordered ${portName}`,
        );

        expect(result).toStrictEqual({
            errorMessage: "reordered Renamed port",
            nodeErrors: [
                {
                    nodeId: "portNode1",
                    message: "reordered Renamed port",
                },
            ],
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
