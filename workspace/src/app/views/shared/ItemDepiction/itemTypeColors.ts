import { convertTaskTypeToItemType, TaskType } from "@ducks/shared/typings";

/**
 * Item-type tile colors for icon depictions, mirroring the `ArtefactTag` badge palette (the
 * `--eccgui-<type>-node-bright` workflow colors) via the shared `ecc-*` Tailwind color tokens:
 * badge background hue as tile background (300 weight), darkest ramp step (900) for the glyph.
 * Rule blocks have no badge color yet and take the free amber ramp.
 */
const tileClassByItemType: Record<string, string> = {
    project: "bg-ecc-magenta-300 text-ecc-magenta-900",
    dataset: "bg-ecc-petrol-300 text-ecc-petrol-900",
    linking: "bg-ecc-cyan-300 text-ecc-cyan-900",
    transform: "bg-ecc-teal-300 text-ecc-teal-900",
    task: "bg-ecc-lime-300 text-ecc-lime-900",
    workflow: "bg-ecc-purple-300 text-ecc-purple-900",
    ruleBlock: "bg-ecc-amber-300 text-ecc-amber-900",
};

/** Background/text classes of the color-coded icon tile for an item (or task) type,
 * `undefined` for unknown types (callers keep the neutral tile then). */
export const itemTypeTileClass = (itemType?: string): string | undefined =>
    itemType ? tileClassByItemType[convertTaskTypeToItemType(itemType as TaskType, true)] : undefined;
