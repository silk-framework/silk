import React from "react";
import { TFunction } from "react-i18next";
import { DATA_TYPES } from "../../../constants";
import Metadata from "../Metadata";
import { RelatedItems } from "../RelatedItems/RelatedItems";
import { TaskConfig } from "../TaskConfig/TaskConfig";
import { TaskActivityOverview } from "../TaskActivityOverview/TaskActivityOverview";
import { ProjectTaskTabView } from "../projectTaskTabView/ProjectTaskTabView";
import { ArtefactManagementOptions } from "../ActionsMenu/ArtefactManagementOptions";
import VariablesWidget from "../VariablesWidget/VariablesWidget";
import { LinkageRuleConfig } from "../../pages/Linking/config/LinkageRuleConfig";
import { IViewActions } from "../../plugins/PluginRegistry";
import { IPluginDetails } from "@ducks/common/typings";
import type { GridBoardItem } from "./GridBoard";
import type { GridLayout } from "./gridEngine";
import { GridTileCard } from "./GridTileCard";

/**
 * The tile ids used across the task-shaped detail pages (Transform, Linking, Task, Dataset,
 * Workflow, RuleBlock). These strings are the localStorage schema key for each tile's persisted
 * position — renaming one silently discards every user's saved layout for that tile, so they are
 * kept in one place and guarded by `taskPageTiles.test.tsx`.
 */
export const TASK_TILE_ID = {
    summary: "summary",
    actions: "actions",
    editor: "editor",
    relatedItems: "relatedItems",
    taskConfig: "taskConfig",
    linkageRuleConfig: "linkageRuleConfig",
    activity: "activity",
    variables: "variables",
    preview: "preview",
} as const;

// ---------------------------------------------------------------------------
// Individual tile factories
// ---------------------------------------------------------------------------

/** "Summary" tile: the shared {@link Metadata} block inside a plain card. */
export const summaryTile = (t: TFunction, opts: { dataTestId?: string } = {}): GridBoardItem => ({
    id: TASK_TILE_ID.summary,
    icon: "item-info",
    title: t("common.words.summary", "Summary"),
    defaultLayout: { x: 0, y: 0, w: 8, h: 3 },
    element: (
        <GridTileCard title={t("common.words.summary", "Summary")} data-test-id={opts.dataTestId}>
            <Metadata />
        </GridTileCard>
    ),
});

/** "Actions" tile: the artefact management options (delete/clone/download/…). */
export const actionsTile = ({
    t,
    projectId,
    taskId,
    itemType,
    notFoundCallback,
    forbiddenCallback,
}: {
    t: TFunction;
    projectId: string;
    taskId?: string;
    itemType: string;
    notFoundCallback?: (notFound: boolean) => void;
    forbiddenCallback?: (forbidden: boolean) => void;
}): GridBoardItem => ({
    id: TASK_TILE_ID.actions,
    icon: "item-moremenu",
    title: t("common.words.actions", "Actions"),
    defaultLayout: { x: 8, y: 0, w: 4, h: 5 },
    element: (
        <ArtefactManagementOptions
            projectId={projectId}
            taskId={taskId}
            itemType={itemType}
            notFoundCallback={notFoundCallback}
            forbiddenCallback={forbiddenCallback}
        />
    ),
});

/**
 * "Editor" tile: the {@link ProjectTaskTabView} for a task plugin (transform / linking / workflow /
 * ruleBlock). ProjectTaskTabView carries its own Card (with the active tab's title + tab bar), so it
 * is rendered bare — wrapping it in a GridTileCard would nest a card in a card and repeat the
 * editor title.
 */
export const editorTile = ({
    t,
    projectId,
    taskId,
    pluginId,
    titleKey,
    titleDefault,
    h,
    viewActions,
}: {
    t: TFunction;
    projectId: string;
    taskId: string;
    pluginId: string;
    titleKey: string;
    titleDefault: string;
    h: number;
    viewActions?: IViewActions;
}): GridBoardItem => ({
    id: TASK_TILE_ID.editor,
    icon: "item-edit",
    title: t(titleKey, titleDefault),
    defaultLayout: { x: 0, y: 3, w: 8, h },
    element: (
        <ProjectTaskTabView
            taskViewConfig={{ pluginId, projectId, taskId }}
            iFrameName={"detail-page-iframe"}
            viewActions={viewActions}
        />
    ),
});

/** "Related items" tile. projectId/taskId are optional — when omitted the widget resolves them from
 * the current project/task selectors, matching the pages that render it bare. */
export const relatedItemsTile = ({
    t,
    projectId,
    taskId,
    messageEventReloadTrigger,
}: {
    t: TFunction;
    projectId?: string;
    taskId?: string;
    messageEventReloadTrigger?: (messageId: string, message: string) => boolean;
}): GridBoardItem => ({
    id: TASK_TILE_ID.relatedItems,
    icon: "toggler-list",
    title: t("RelatedItems.title", "Related items"),
    defaultLayout: { x: 8, y: 0, w: 4, h: 5 },
    element: (
        <RelatedItems projectId={projectId} taskId={taskId} messageEventReloadTrigger={messageEventReloadTrigger} />
    ),
});

/** "Configuration" tile: the task's parameter configuration. */
export const taskConfigTile = ({
    t,
    projectId,
    taskId,
    pluginDataCallback,
    defaultLayout = { x: 8, y: 5, w: 4, h: 5 },
}: {
    t: TFunction;
    projectId: string;
    taskId: string;
    pluginDataCallback?: (task: IPluginDetails) => void;
    defaultLayout?: GridLayout;
}): GridBoardItem => ({
    id: TASK_TILE_ID.taskConfig,
    icon: "item-settings",
    title: t("widget.TaskConfigWidget.title", "Configuration"),
    defaultLayout,
    element: <TaskConfig projectId={projectId} taskId={taskId} pluginDataCallback={pluginDataCallback} />,
});

/** "Configuration: Linkage rule" tile — Linking page only. */
export const linkageRuleConfigTile = ({
    t,
    projectId,
    taskId,
    defaultLayout = { x: 8, y: 10, w: 4, h: 5 },
}: {
    t: TFunction;
    projectId: string;
    taskId: string;
    defaultLayout?: GridLayout;
}): GridBoardItem => ({
    id: TASK_TILE_ID.linkageRuleConfig,
    icon: "artefact-linking",
    title: t("widget.LinkingRuleConfigWidget.title", "Configuration: Linkage rule"),
    defaultLayout,
    element: <LinkageRuleConfig projectId={projectId} linkingTaskId={taskId} />,
});

/** "Activities" tile: the task activity overview. */
export const activityTile = ({
    t,
    projectId,
    taskId,
    defaultLayout = { x: 8, y: 10, w: 4, h: 4 },
}: {
    t: TFunction;
    projectId: string;
    taskId: string;
    defaultLayout?: GridLayout;
}): GridBoardItem => ({
    id: TASK_TILE_ID.activity,
    icon: "application-activities",
    title: t("widget.TaskActivityOverview.title", "Activities"),
    defaultLayout,
    element: <TaskActivityOverview projectId={projectId} taskId={taskId} />,
});

/** "Execution variables" tile. */
export const variablesTile = ({
    t,
    projectId,
    taskId,
    defaultLayout = { x: 8, y: 14, w: 4, h: 5 },
}: {
    t: TFunction;
    projectId: string;
    taskId?: string;
    defaultLayout?: GridLayout;
}): GridBoardItem => ({
    id: TASK_TILE_ID.variables,
    icon: "data-string",
    title: t("widget.VariableWidget.title.execution", "Execution variables"),
    defaultLayout,
    element: <VariablesWidget projectId={projectId} taskId={taskId} />,
});

// ---------------------------------------------------------------------------
// Per-page tile-array builders. The pages call these so the tile composition
// (and, critically, the id order that keys persisted layouts) lives in one
// place. Page-specific pieces (editor plugin, data preview, save hook) are
// passed in as parameters.
// ---------------------------------------------------------------------------

interface CommonTileDeps {
    t: TFunction;
    projectId: string;
    taskId: string;
    notFoundCallback: (notFound: boolean) => void;
    forbiddenCallback: (forbidden: boolean) => void;
}

export const buildTransformTiles = ({
    t,
    projectId,
    taskId,
    notFoundCallback,
    forbiddenCallback,
    updateBreadcrumbsExtensions,
}: CommonTileDeps & { updateBreadcrumbsExtensions: IViewActions["addLocalBreadcrumbs"] }): GridBoardItem[] => [
    summaryTile(t),
    actionsTile({ t, projectId, taskId, itemType: DATA_TYPES.TRANSFORM, notFoundCallback, forbiddenCallback }),
    editorTile({
        t,
        projectId,
        taskId,
        pluginId: "transform",
        titleKey: "pages.transform.title",
        titleDefault: "Mapping editor",
        h: 13,
        viewActions: { addLocalBreadcrumbs: updateBreadcrumbsExtensions },
    }),
    relatedItemsTile({ t, projectId, taskId }),
    taskConfigTile({ t, projectId, taskId }),
    activityTile({ t, projectId, taskId }),
    variablesTile({ t, projectId, taskId }),
];

export const buildLinkingTiles = ({
    t,
    projectId,
    taskId,
    notFoundCallback,
    forbiddenCallback,
}: CommonTileDeps): GridBoardItem[] => [
    summaryTile(t),
    actionsTile({ t, projectId, taskId, itemType: DATA_TYPES.LINKING, notFoundCallback, forbiddenCallback }),
    editorTile({
        t,
        projectId,
        taskId,
        pluginId: "linking",
        titleKey: "pages.linking.title",
        titleDefault: "Linking editor",
        h: 16,
    }),
    relatedItemsTile({ t }),
    taskConfigTile({ t, projectId, taskId }),
    linkageRuleConfigTile({ t, projectId, taskId }),
    activityTile({ t, projectId, taskId, defaultLayout: { x: 8, y: 15, w: 4, h: 4 } }),
    variablesTile({ t, projectId, taskId, defaultLayout: { x: 8, y: 19, w: 4, h: 5 } }),
];

export const buildTaskTiles = ({
    t,
    projectId,
    taskId,
    notFoundCallback,
    forbiddenCallback,
    pluginDataCallback,
}: CommonTileDeps & { pluginDataCallback: (task: IPluginDetails) => void }): GridBoardItem[] => [
    summaryTile(t),
    actionsTile({ t, projectId, taskId, itemType: DATA_TYPES.TASK, notFoundCallback, forbiddenCallback }),
    taskConfigTile({ t, projectId, taskId, pluginDataCallback, defaultLayout: { x: 0, y: 3, w: 8, h: 8 } }),
    relatedItemsTile({ t, projectId, taskId }),
    activityTile({ t, projectId, taskId, defaultLayout: { x: 8, y: 5, w: 4, h: 6 } }),
];

export const buildDatasetTiles = ({
    t,
    projectId,
    taskId,
    notFoundCallback,
    forbiddenCallback,
    pluginDataCallback,
    mainContent,
}: CommonTileDeps & {
    pluginDataCallback: (task: IPluginDetails) => void;
    mainContent: React.ReactNode;
}): GridBoardItem[] => {
    const items: GridBoardItem[] = [
        summaryTile(t),
        actionsTile({ t, projectId, taskId, itemType: DATA_TYPES.DATASET, notFoundCallback, forbiddenCallback }),
        relatedItemsTile({ t }),
        taskConfigTile({ t, projectId, taskId, pluginDataCallback }),
        activityTile({ t, projectId, taskId }),
    ];
    if (mainContent) {
        items.splice(1, 0, {
            id: TASK_TILE_ID.preview,
            icon: "item-viewdetails",
            title: t("pages.dataset.title", "Data preview"),
            defaultLayout: { x: 0, y: 3, w: 8, h: 11 },
            element: mainContent,
        });
    }
    return items;
};

export const buildWorkflowTiles = ({
    t,
    projectId,
    taskId,
    notFoundCallback,
    forbiddenCallback,
    onSave,
    messageEventReloadTrigger,
}: CommonTileDeps & {
    onSave: () => void;
    messageEventReloadTrigger: (messageId: string, message: string) => boolean;
}): GridBoardItem[] => [
    summaryTile(t, { dataTestId: "workflow-summary-tile" }),
    actionsTile({ t, projectId, taskId, itemType: DATA_TYPES.WORKFLOW, notFoundCallback, forbiddenCallback }),
    editorTile({
        t,
        projectId,
        taskId,
        pluginId: "workflow",
        titleKey: "widget.WorkflowEditor.title",
        titleDefault: "Workflow editor",
        h: 13,
        viewActions: { onSave },
    }),
    relatedItemsTile({ t, messageEventReloadTrigger }),
    variablesTile({ t, projectId, taskId, defaultLayout: { x: 8, y: 5, w: 4, h: 5 } }),
];

export const buildRuleBlockTiles = ({
    t,
    projectId,
    taskId,
    notFoundCallback,
    forbiddenCallback,
    updateBreadcrumbsExtensions,
}: CommonTileDeps & { updateBreadcrumbsExtensions: IViewActions["addLocalBreadcrumbs"] }): GridBoardItem[] => [
    summaryTile(t),
    actionsTile({ t, projectId, taskId, itemType: DATA_TYPES.RULE_BLOCK, notFoundCallback, forbiddenCallback }),
    editorTile({
        t,
        projectId,
        taskId,
        pluginId: "ruleBlock",
        titleKey: "pages.ruleBlock.title",
        titleDefault: "Rule block editor",
        h: 13,
        viewActions: { addLocalBreadcrumbs: updateBreadcrumbsExtensions },
    }),
    relatedItemsTile({ t, projectId, taskId }),
    activityTile({ t, projectId, taskId }),
];
