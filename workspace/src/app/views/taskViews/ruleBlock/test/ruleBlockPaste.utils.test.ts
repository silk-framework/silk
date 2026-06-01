import type {
    RuleClipboardTask,
    RuleClipboardTaskData,
    RuleNodeCopySerialization,
} from "../../../shared/RuleEditor/model/RuleEditorModel.typings";
import type { IRuleBlockPort } from "../ruleBlock.types";
import ruleBlockPasteUtils from "../ruleBlockPaste.utils";

const inputPortNodeMetaData = (port: IRuleBlockPort) => ({
    label: port.label,
    description: port.description,
    tags: [String(port.displayOrder)],
});

const pathNode = (
    nodeId: string,
    pluginId: "sourcePathInput" | "targetPathInput",
    path: string,
): RuleNodeCopySerialization => ({
    nodeId,
    pluginId,
    pluginType: "PathInputOperator",
    position: { x: 0, y: 0 },
    parameters: {
        path,
    },
    inputHandleIds: [],
});

const transformNode = (nodeId: string, inputs: string[] = []): RuleNodeCopySerialization => ({
    nodeId,
    pluginId: "concat",
    pluginType: "TransformOperator",
    position: { x: 100, y: 50 },
    parameters: {},
    inputHandleIds: inputs.map((_, idx) => String(idx)),
});

const comparisonNode = (): RuleNodeCopySerialization => ({
    nodeId: "cmp",
    pluginId: "levenshtein",
    pluginType: "ComparisonOperator",
    position: { x: 0, y: 0 },
    parameters: {},
    inputHandleIds: ["0", "1"],
});

const inputPortNode = (nodeId: string, portId: string): RuleNodeCopySerialization => ({
    nodeId,
    pluginId: "inputPort",
    pluginType: "InputPortOperator",
    position: { x: 0, y: 0 },
    parameters: {
        portId,
    },
    inputHandleIds: [],
});

const clipboardTask = (
    taskData: RuleClipboardTaskData,
    {
        project = "project1",
        task = "task1",
        editorData,
    }: {
        project?: string;
        task?: string;
        editorData?: unknown;
    } = {},
): RuleClipboardTask => ({
    data: taskData,
    metaData: {
        project,
        task,
    },
    editorData,
});

const preparePaste = (
    task: RuleClipboardTask,
    existingPorts: IRuleBlockPort[] = [],
    createdIds: string[] = [],
) =>
    ruleBlockPasteUtils.prepareRuleBlockClipboardPaste(task, {
        currentProjectId: "project1",
        currentTaskId: "task1",
        existingPorts,
        createInputPortId: () => {
            const id = `generatedPort${createdIds.length + 1}`;
            createdIds.push(id);
            return id;
        },
        inputPortNodeMetaData,
    });

describe("ruleBlockPasteUtils", () => {
    it("should reject pasted comparison or aggregation content", () => {
        expect(() =>
            preparePaste(clipboardTask({
                nodes: [comparisonNode()],
                edges: [],
            })),
        ).toThrow("Only transform-compatible subtrees can be pasted into a rule block.");
    });

    it("should allow source-side and target-side path fragments in one paste and keep them as separate input ports", () => {
        const preparedPaste = preparePaste(
            clipboardTask({
                nodes: [
                    pathNode("sourcePath", "sourcePathInput", "foaf:name"),
                    pathNode("targetPath", "targetPathInput", "foaf:name"),
                ],
                edges: [],
            }),
        );

        expect(preparedPaste.createdPorts).toStrictEqual([
            {
                id: "generatedPort1",
                label: "foaf:name",
                description: "",
                displayOrder: 1,
                deprecated: false,
            },
            {
                id: "generatedPort2",
                label: "foaf:name",
                description: "",
                displayOrder: 2,
                deprecated: false,
            },
        ]);
        expect(
            preparedPaste.taskData.nodes.map((node) => node.parameters?.portId),
        ).toStrictEqual(["generatedPort1", "generatedPort2"]);
    });

    it("should reuse existing logical input ports when pasting from the same rule block", () => {
        const existingPort = {
            id: "inputPortA",
            label: "Input A",
            description: "Input description",
            displayOrder: 2,
            deprecated: false,
        };

        const preparedPaste = preparePaste(
            clipboardTask({
                nodes: [
                    {
                        ...inputPortNode("inputA", "inputPortA"),
                        nodeMetaData: {
                            label: "Stale label",
                            description: "Stale description",
                            tags: ["stale"],
                        },
                    },
                ],
                edges: [],
            }),
            [existingPort],
        );

        expect(preparedPaste.createdPorts).toStrictEqual([]);
        expect(preparedPaste.taskData.nodes).toStrictEqual([
            expect.objectContaining({
                nodeId: "inputA",
                pluginType: "InputPortOperator",
                parameters: {
                    portId: "inputPortA",
                },
                nodeMetaData: inputPortNodeMetaData(existingPort),
            }),
        ]);
    });

    it("should create new logical input ports when pasting copied input-port nodes from a different rule block", () => {
        const preparedPaste = preparePaste(
            clipboardTask(
                {
                    nodes: [inputPortNode("inputA", "externalPortA")],
                    edges: [],
                },
                {
                    task: "externalTask",
                    editorData: {
                        inputPorts: [
                            {
                                id: "externalPortA",
                                label: "External input",
                                description: "External description",
                                displayOrder: 7,
                                deprecated: true,
                            },
                        ],
                    },
                },
            ),
            [{ id: "existingPort", label: "Existing", description: "", displayOrder: 4, deprecated: false }],
        );

        expect(preparedPaste.createdPorts).toStrictEqual([
            {
                id: "generatedPort1",
                label: "External input",
                description: "External description",
                displayOrder: 5,
                deprecated: true,
            },
        ]);
        expect(preparedPaste.taskData.nodes).toStrictEqual([
            expect.objectContaining({
                nodeId: "inputA",
                pluginType: "InputPortOperator",
                parameters: {
                    portId: "generatedPort1",
                },
                nodeMetaData: inputPortNodeMetaData(preparedPaste.createdPorts[0]),
            }),
        ]);
    });

    it("should convert repeated identical pasted paths into input ports and deduplicate them within one paste", () => {
        const preparedPaste = preparePaste(
            clipboardTask({
                nodes: [
                    pathNode("pathA", "sourcePathInput", "foaf:name"),
                    pathNode("pathB", "sourcePathInput", "foaf:name"),
                    transformNode("transform", ["pathA", "pathB"]),
                ],
                edges: [
                    { source: "pathA", target: "transform", targetHandle: "0", type: "value" },
                    { source: "pathB", target: "transform", targetHandle: "1", type: "value" },
                ],
            }),
            [{ id: "existingPort", label: "Existing", description: "", displayOrder: 4, deprecated: false }],
        );

        expect(preparedPaste.createdPorts).toStrictEqual([
            {
                id: "generatedPort1",
                label: "foaf:name",
                description: "",
                displayOrder: 5,
                deprecated: false,
            },
        ]);
        expect(
            preparedPaste.taskData.nodes
                .filter((node) => node.pluginType === "InputPortOperator")
                .map((node) => node.parameters?.portId),
        ).toStrictEqual(["generatedPort1", "generatedPort1"]);
        expect(
            preparedPaste.taskData.nodes.find((node) => node.nodeId === "transform"),
        ).toMatchObject({
            pluginType: "TransformOperator",
            pluginId: "concat",
        });
    });
});
