/**
 * What the changes page tells about the change journal, shown in its help dialog: the user documentation of the journal
 * until a page of the documentation site covers it, which is then linked from here.
 */
export const changeJournalHelpMarkdown = `
This page is the project's change journal, and it is there for two reasons. Almost every change to the project can be undone from here, so a mistake, your own or anyone else's, costs no more than a revert. And an AI agent working on the project through the MCP server changes it without asking you for each step, so everything an agent did is listed here for you to review.

Changes to the project's content are recorded here: tasks added, updated or removed, single mapping rules, workflow nodes and connections, variables, tags and uploaded or deleted files, whether made in the workbench, through the REST API or by an AI agent. Project settings such as its label or prefixes are not. Each entry tells who made the change, when, and what it changed. Workflow runs are recorded as well, so you can see which changes a run already used. A run itself cannot be reverted: what it wrote, e.g. to a file, an RDF graph or an external database, stays even after the changes it used are reverted. Everyone who can open the project sees its changes and can review and revert them. The list can be searched, filtered by kind, state, origin, user and task, and sorted.

### Changes made by an agent

A change that an AI agent made through the MCP server is marked as *unreviewed* until you have reviewed it. To review, look through the unreviewed changes, revert the ones you do not want, and click **Mark all as reviewed**, which marks every change in the list as reviewed; reload the list first if changes may have been made since it was loaded. **Mark as reviewed up to here** in the menu of an unreviewed change marks it and all older changes as reviewed and leaves the newer ones for later; a change cannot be reviewed while an older one is still unreviewed. **Revert unreviewed** reverts all unreviewed changes at once, newest first. The MCP server has no tool for marking changes as reviewed, so an agent cannot approve its own work.

An agent does not run a workflow on its own: a run would use the unreviewed changes, and what it writes cannot be reverted. The request is recorded as a proposed run instead, and the agent is told to wait for your review. Once you have marked the changes as reviewed, the agent's next attempt runs the workflow; reverting the proposed run rejects it. Every further run the agent requests is proposed again. The administrator can switch this review gate off in the configuration.

### Reverting a change

A revert undoes exactly what the change did: reverting the addition of a mapping rule removes that rule and nothing else. The revert is recorded as a new change, and reverting that one redoes the original change. **Revert this and all newer changes** returns to the state from before a change. Like **Revert unreviewed**, it skips changes that cannot be reverted and stops at the first one that conflicts; the result tells which changes were reverted.

A change can be reverted while what it changed is still as the change left it. Otherwise the revert is disabled and its tooltip tells why, e.g. that the task has been changed since or that another task now references it; reverting the newer changes first may clear that. Overwriting or deleting a file and running a workflow cannot be reverted at all, nor can a proposed run once it has been run.
`;
