import ruleBlockUtils from "./ruleBlock.utils";
import { IRuleOperatorNode } from "../../shared/RuleEditor/RuleEditor.typings";

describe("ruleBlockUtils", () => {
    it("should reject duplicate display orders across different rule block ports", () => {
        const result = ruleBlockUtils.collectPortDefinitions(
            [],
            [
                inputPortNode("portNode1", "firstPort", 1),
                inputPortNode("portNode2", "secondPort", 1),
            ],
            "invalid display order",
            (portId) => `conflicting definition for ${portId}`,
            (displayOrder) => `duplicate display order ${displayOrder}`,
        );

        expect(result.portDefinitions).toBeUndefined();
        expect(result.nodeErrors).toStrictEqual([
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
            (displayOrder) => `duplicate display order ${displayOrder}`,
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
