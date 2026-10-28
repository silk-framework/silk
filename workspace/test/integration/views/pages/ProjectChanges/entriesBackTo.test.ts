import { entriesBackTo } from "../../../../../src/app/views/pages/ProjectChanges/changeListUtils";
import { IChangeEntry } from "../../../../../src/app/views/pages/ProjectChanges/changesRequests";

describe("entriesBackTo", () => {
    /** A change, or the revert of change `reverts`; only what the function reads is set. */
    const entry = (seq: number, reverts?: number, revertible: boolean = true) =>
        ({ seq, reverts, revertible }) as IChangeEntry;

    // The entries as the server lists them, newest first, the seq to go back to before, and the seqs to revert
    it.each<[string, IChangeEntry[], number, number[]]>([
        ["a plain history to the middle", [entry(3), entry(2), entry(1)], 2, [3, 2]],
        ["a plain history to the oldest change", [entry(3), entry(2), entry(1)], 1, [3, 2, 1]],
        ["a plain history to the newest change", [entry(3), entry(2), entry(1)], 3, [3]],
        ["nothing for a later change that is reverted already", [entry(3, 2), entry(2), entry(1)], 2, []],
        ["only the older change when the later one is reverted already", [entry(3, 2), entry(2), entry(1)], 1, [1]],
        ["the revert of an earlier change, which redoes it", [entry(3, 1), entry(2), entry(1)], 2, [3, 2]],
        ["a revert itself", [entry(3, 1), entry(2), entry(1)], 3, [3]],
        ["the revert of a revert", [entry(3, 2), entry(2, 1), entry(1)], 3, [3]],
        ["nothing when a change is redone as before", [entry(3, 2), entry(2, 1), entry(1)], 2, []],
        ["the newest link of a chain to before its change", [entry(3, 2), entry(2, 1), entry(1)], 1, [3]],
        ["a chain that spans the change to go back to", [entry(4, 2), entry(3), entry(2, 1), entry(1)], 3, [4, 3]],
        [
            "also what cannot be reverted, to be announced as skipped",
            [entry(3), entry(2, undefined, false), entry(1)],
            2,
            [3, 2],
        ],
        ["newest first whatever the order of the entries", [entry(1), entry(3), entry(2)], 2, [3, 2]],
        ["nothing for a seq after the newest change", [entry(3), entry(2), entry(1)], 99, []],
        ["nothing of an empty list", [], 1, []],
        // The reverted change has left the journal, which keeps a limited number of entries
        ["a revert whose change is no longer listed", [entry(13), entry(12, 3), entry(11)], 11, [13, 12, 11]],
        ["to a revert whose change is no longer listed", [entry(13), entry(12, 3), entry(11)], 12, [13, 12]],
    ])("should revert %s", (_, entries, seq, expected) => {
        expect(entriesBackTo(entries, seq).map((reverted) => reverted.seq)).toEqual(expected);
    });
});
