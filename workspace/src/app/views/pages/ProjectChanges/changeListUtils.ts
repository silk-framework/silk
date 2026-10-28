import { ValidIconName } from "@eccenca/gui-elements/src/components/Icon/canonicalIconNames";
import { IChangeEntry } from "./changesRequests";

/** The last segment of a user URI, e.g. 'alice' for 'urn:user:alice'. */
export const userDisplayName = (uri: string): string => {
    const idx = Math.max(uri.lastIndexOf("/"), uri.lastIndexOf(":"), uri.lastIndexOf("#"));
    return idx >= 0 && idx < uri.length - 1 ? uri.substring(idx + 1) : uri;
};

export type ChangeKind = "added" | "updated" | "removed" | "run";

/** The kind of change by its type name, e.g. 'AddMapping' adds, 'ResourceDeleted' removes, 'WorkflowExecuted' and its proposal are runs. */
export const changeKind = (type: string): ChangeKind => {
    if (type === "WorkflowExecuted" || type === "ProposedWorkflowRun") {
        return "run";
    } else if (type.startsWith("Add") || type === "ResourceCreated") {
        return "added";
    } else if (
        type.startsWith("Remove") ||
        type.startsWith("Discarded") ||
        type === "ResourceDeleted" ||
        type === "DisconnectWorkflowNodes"
    ) {
        return "removed";
    } else {
        return "updated";
    }
};

export const kindIntent: Record<ChangeKind, "success" | "danger" | "info" | undefined> = {
    added: "success",
    removed: "danger",
    run: "info",
    updated: undefined,
};

/** The icon of a link by its id, as handed out by the server. */
export const linkIcon = (id: string): ValidIconName => {
    switch (id) {
        case "rule":
            return "application-mapping";
        case "report":
            return "artefact-report";
        case "download":
            return "item-download";
        default:
            return "item-viewdetails";
    }
};

/**
 * Whether an entry has an inverse that has not been applied yet. A batch attempts every such entry: a conflict the
 * server reports may clear once the newer entries are reverted, as the batch reverts newest first.
 */
export const hasInverse = (entry: IChangeEntry): boolean => entry.revertible && entry.revertedBy == null;

/**
 * The entries to revert so that the project returns to its state before change `seq`, newest first. A change, the
 * revert of it, the revert of that revert and so on form a chain that toggles the change on and off, so per chain the
 * newest link is reverted when the change is in effect now but was not before `seq`, or the other way round. Links that
 * cannot be reverted are included, so that the caller can announce them as skipped. A revert whose change is no longer
 * listed starts a chain of its own.
 */
export const entriesBackTo = (entries: IChangeEntry[], seq: number): IChangeEntry[] => {
    const listed = new Set(entries.map((entry) => entry.seq));
    const revertOf = new Map<number, IChangeEntry>();
    entries.forEach((entry) => {
        if (entry.reverts != null) {
            revertOf.set(entry.reverts, entry);
        }
    });
    const heads: IChangeEntry[] = [];
    entries
        .filter((change) => change.reverts == null || !listed.has(change.reverts))
        .forEach((change) => {
            let head = change;
            let toggles = 0;
            let togglesBefore = 0;
            for (let next = revertOf.get(head.seq); next; next = revertOf.get(head.seq)) {
                toggles++;
                if (next.seq < seq) {
                    togglesBefore++;
                }
                head = next;
            }
            const inEffectNow = toggles % 2 === 0;
            const inEffectBefore = change.seq < seq && togglesBefore % 2 === 0;
            if (inEffectNow !== inEffectBefore) {
                heads.push(head);
            }
        });
    return heads.sort((a, b) => b.seq - a.seq);
};
