/**
 * Single source of truth for the first-class artefact types the header can create.
 *
 * Consumed by:
 *  - `CreateSplitButton` — the caret dropdown's category list (icon + label).
 *  - `useKeyBoardHeaderShortcuts` — the "c <suffix>" create chords.
 *  - `KeyboardShortcutsModal` — the documented create-shortcut rows.
 *  - `SearchList` — the direct-create task-plugin lookup (types that map to a task plugin).
 *
 * Labels point at the shared `common.dataTypes.*` keys (rendered via `uppercaseFirstChar`), NOT the
 * Filterbar's private `widget.Filterbar.subsections.valueLabels.itemType.*` value labels, so the
 * create surfaces no longer borrow an unrelated widget's i18n.
 */
export interface ArtefactType {
    /** Artefact dtype / itemType, e.g. "project". */
    dtype: string;
    /** Icon name in the shared icon set. */
    icon: string;
    /** i18n key for the singular type label (render via `uppercaseFirstChar`). */
    labelKey: string;
    /**
     * Task plugin id for the types created directly as a task (dataset/workflow/transform/linking).
     * Absent for `project` (its own artefact kind) and `task` (opens the create dialog on that
     * category rather than instantiating a fixed plugin).
     */
    pluginId?: string;
    /** Suffix of the "c <suffix>" create hotkey chord (e.g. "p" for the `c p` chord). */
    hotkeySuffix: string;
}

/**
 * Order matters: this is the order the categories appear in the Create menu and the keyboard
 * shortcuts modal.
 */
export const artefactTypes: ArtefactType[] = [
    { dtype: "project", icon: "artefact-project", labelKey: "common.dataTypes.project", hotkeySuffix: "p" },
    {
        dtype: "dataset",
        icon: "artefact-dataset",
        labelKey: "common.dataTypes.dataset",
        pluginId: "dataset",
        hotkeySuffix: "d",
    },
    {
        dtype: "workflow",
        icon: "artefact-workflow",
        labelKey: "common.dataTypes.workflow",
        pluginId: "workflow",
        hotkeySuffix: "w",
    },
    {
        dtype: "transform",
        icon: "artefact-transform",
        labelKey: "common.dataTypes.transform",
        pluginId: "transform",
        hotkeySuffix: "t",
    },
    {
        dtype: "linking",
        icon: "artefact-linking",
        labelKey: "common.dataTypes.linking",
        pluginId: "linking",
        hotkeySuffix: "l",
    },
    { dtype: "task", icon: "artefact-task", labelKey: "common.dataTypes.task", hotkeySuffix: "o" },
];
