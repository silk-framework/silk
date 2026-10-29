import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import { TaskConfigPreview } from "../TaskConfigPreview";
import { INPUT_TYPES } from "../../../../constants";
import { IPluginDetails } from "@ducks/common/typings";
import { IProjectTask } from "@ducks/shared/typings";
import { renderWrapper } from "../../../../../../test/integration/TestHelper";

describe("TaskConfigPreview", () => {
    beforeEach(() => {
        Object.defineProperty(navigator, "clipboard", {
            configurable: true,
            value: {
                writeText: jest.fn().mockResolvedValue(undefined),
            },
        });
    });

    it("shows a copy button only for graph uri parameters and copies the graph URI", async () => {
        const taskData = {
            data: {
                parameters: {
                    endpoint: "default",
                    graph: "https://example.org/graphs/test",
                },
                type: "eccencaDataPlatform",
            },
            id: "dataset1",
            metadata: {},
            project: "project1",
            taskType: "Dataset",
        } as IProjectTask;
        const taskDescription = {
            categories: [],
            description: "",
            pluginId: "eccencaDataPlatform",
            properties: {
                endpoint: {
                    advanced: false,
                    description: "",
                    parameterType: INPUT_TYPES.STRING,
                    title: "Endpoint",
                    type: "string",
                    value: "default",
                    visibleInDialog: true,
                },
                graph: {
                    advanced: false,
                    description: "",
                    parameterType: INPUT_TYPES.GRAPH_URI,
                    title: "Graph",
                    type: "string",
                    value: null,
                    visibleInDialog: true,
                },
            },
            required: ["graph"],
            taskType: "Dataset",
            title: "Knowledge Graph",
            type: "object",
        } as IPluginDetails;

        const { container } = renderWrapper(
            <TaskConfigPreview taskData={taskData} taskDescription={taskDescription} />,
        );

        const copyButtons = container.querySelectorAll('[data-test-id^="task-config-preview-copy-button-"]');
        expect(copyButtons).toHaveLength(1);

        fireEvent.click(copyButtons[0]);

        expect(navigator.clipboard.writeText).toHaveBeenCalledWith("https://example.org/graphs/test");
        expect(await screen.findByTitle("Copied")).toBeInTheDocument();
    });
});
