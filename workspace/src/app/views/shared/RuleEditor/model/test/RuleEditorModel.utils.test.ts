import { ruleEditorModelUtilsFactory } from "../RuleEditorModel.utils";
import { Edge, Node, XYPosition } from "react-flow-renderer/dist/types";

describe("Rule editor model utils", () => {
    const utils = ruleEditorModelUtilsFactory();

    it("should remove undefined input ports from array", () => {
        const x = "x";
        const n = undefined;
        const test = (inputArray: ("x" | undefined)[], expectedArray: ("x" | undefined)[]) => {
            utils.adaptInputArray(inputArray);
            expect(inputArray).toStrictEqual(expectedArray);
        };
        test([n, x, n], [n, x]);
        test([x, n, n], [x]);
        test([x, n, x], [x, n, x]);
        test([x, n, n, x, n], [x, n, n, x]);
        test([], []);
        test([x], [x]);
        test([n], []);
    });

    it("should auto-layout a graph", () => {
        const initialNodes = [{ id: "nodeA" }, { id: "nodeB" }, { id: "nodeC" }] as any as Node[];
        const layout = (edges: Edge[], nodes: Node[] = initialNodes): XYPosition[] => {
            const nodeMap = utils.autoLayout([...nodes, ...edges], 1, "id");
            return nodes.map((n) => nodeMap.get(n.id)!!);
        };
        const unconnected = layout([]);
        expect(unconnected).toHaveLength(initialNodes.length);
        // All nodes have the same x coordinate
        expect(unconnected[0].x).toBe(unconnected[1].x);
        expect(unconnected[1].x).toBe(unconnected[2].x);
        // but different y coordinates
        expect(unconnected[0].y).toBeLessThan(unconnected[1].y);
        expect(unconnected[1].y).toBeLessThan(unconnected[2].y);
        // Giving initial coordinates should put them closer to the original positions
        const unconnectedWithInitialPosition = layout(
            [],
            initialNodes.map((n, idx) => ({ ...n, position: { x: 100 * idx - 1000, y: -100 * idx - 1000 } }))
        );
        expect(unconnectedWithInitialPosition[0]).not.toStrictEqual(unconnected[0]);
    });
});
