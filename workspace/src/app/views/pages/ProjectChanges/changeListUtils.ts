import { IChangeEntry } from "./changesRequests";

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
