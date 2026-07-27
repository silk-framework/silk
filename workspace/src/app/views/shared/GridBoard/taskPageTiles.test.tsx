import React from "react";
import { TFunction } from "react-i18next";
import {
    buildTransformTiles,
    buildLinkingTiles,
    buildTaskTiles,
    buildDatasetTiles,
    buildWorkflowTiles,
    buildRuleBlockTiles,
} from "./taskPageTiles";

// The tile factories create React elements for the real page widgets. This test only inspects the
// resulting tile ids (never renders), so the widgets are stubbed to keep the import side-effect-free.
// (`jest.mock` calls are hoisted above the imports by babel-jest.)
jest.mock("../Metadata", () => ({ __esModule: true, default: () => null }));
jest.mock("../RelatedItems/RelatedItems", () => ({ RelatedItems: () => null }));
jest.mock("../TaskConfig/TaskConfig", () => ({ TaskConfig: () => null }));
jest.mock("../TaskActivityOverview/TaskActivityOverview", () => ({ TaskActivityOverview: () => null }));
jest.mock("../projectTaskTabView/ProjectTaskTabView", () => ({ ProjectTaskTabView: () => null }));
jest.mock("../ActionsMenu/ArtefactManagementOptions", () => ({ ArtefactManagementOptions: () => null }));
jest.mock("../VariablesWidget/VariablesWidget", () => ({ __esModule: true, default: () => null }));
jest.mock("../../pages/Linking/config/LinkageRuleConfig", () => ({ LinkageRuleConfig: () => null }));

// A minimal translation stub that returns the fallback string.
const t = ((_key: string, fallback: string) => fallback) as unknown as TFunction;
const noop = () => undefined;

const commonDeps = {
    t,
    projectId: "p",
    taskId: "tk",
    notFoundCallback: noop,
    forbiddenCallback: noop,
};

const ids = (items: { id: string }[]) => items.map((item) => item.id);

describe("task detail page tile ids", () => {
    // These id lists ARE the localStorage layout schema — a rename silently discards saved layouts.
    // If a tile id legitimately changes, update this test deliberately (and consider migrating
    // persisted keys), do not just make CI green.
    it("Transform page tile ids are stable", () => {
        expect(ids(buildTransformTiles({ ...commonDeps, updateBreadcrumbsExtensions: noop }))).toEqual([
            "summary",
            "actions",
            "editor",
            "relatedItems",
            "taskConfig",
            "activity",
            "variables",
        ]);
    });

    it("Linking page tile ids are stable", () => {
        expect(ids(buildLinkingTiles(commonDeps))).toEqual([
            "summary",
            "actions",
            "editor",
            "relatedItems",
            "taskConfig",
            "linkageRuleConfig",
            "activity",
            "variables",
        ]);
    });

    it("Task page tile ids are stable", () => {
        expect(ids(buildTaskTiles({ ...commonDeps, pluginDataCallback: noop }))).toEqual([
            "summary",
            "actions",
            "taskConfig",
            "relatedItems",
            "activity",
        ]);
    });

    it("Dataset page tile ids are stable (with and without the data preview)", () => {
        expect(ids(buildDatasetTiles({ ...commonDeps, pluginDataCallback: noop, mainContent: null }))).toEqual([
            "summary",
            "actions",
            "relatedItems",
            "taskConfig",
            "activity",
        ]);
        expect(ids(buildDatasetTiles({ ...commonDeps, pluginDataCallback: noop, mainContent: <div /> }))).toEqual([
            "summary",
            "preview",
            "actions",
            "relatedItems",
            "taskConfig",
            "activity",
        ]);
    });

    it("Workflow page tile ids are stable", () => {
        expect(ids(buildWorkflowTiles({ ...commonDeps, onSave: noop, messageEventReloadTrigger: () => true }))).toEqual(
            ["summary", "actions", "editor", "relatedItems", "variables"],
        );
    });

    it("RuleBlock page tile ids are stable", () => {
        expect(ids(buildRuleBlockTiles({ ...commonDeps, updateBreadcrumbsExtensions: noop }))).toEqual([
            "summary",
            "actions",
            "editor",
            "relatedItems",
            "activity",
        ]);
    });
});
