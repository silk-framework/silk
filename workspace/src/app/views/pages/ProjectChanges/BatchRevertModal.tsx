import React from "react";
import { useTranslation } from "react-i18next";
import { Notification } from "@eccenca/gui-elements";
import DeleteModal from "../../shared/modals/DeleteModal";
import useErrorHandler from "../../../hooks/useErrorHandler";
import { useModalError } from "../../../hooks/useModalError";
import { ErrorResponse } from "../../../services/fetch/responseInterceptor";
import { IChangeEntry, IRevertOutcome, requestRevertChanges, requestRevertConflicts } from "./changesRequests";
import { hasInverse } from "./changeListUtils";

interface IProps {
    projectId: string;
    title: string;
    confirmText: string;
    /** The entries in scope, of which the revertible ones are attempted. */
    entries: IChangeEntry[];
    onClose: () => void;
    /** Called with the outcomes once the batch has been reverted. */
    onReverted: (results: IRevertOutcome[]) => Promise<void>;
}

/**
 * Asks to confirm a batch revert. When it opens, it asks whether the newest entry of the batch conflicts now, which
 * would stop the batch before reverting anything; the revert is offered once that is answered.
 */
const BatchRevertModal = ({ projectId, title, confirmText, entries, onClose, onReverted }: IProps) => {
    const [t] = useTranslation();
    const { registerError } = useErrorHandler();
    const head = entries.find(hasInverse);
    // True until the server has answered whether the batch can start
    const [checking, setChecking] = React.useState<boolean>(head != null);
    // Why the batch would stop before reverting anything; undefined while checking or when it can start
    const [blocked, setBlocked] = React.useState<string | undefined>(undefined);
    const [loading, setLoading] = React.useState<boolean>(false);
    const [error, setError] = React.useState<ErrorResponse | undefined>(undefined);
    const displayError = useModalError({ setError });

    React.useEffect(() => {
        if (!head) {
            return;
        }
        let closed = false;
        (async () => {
            let reason: string | undefined;
            try {
                reason = (await requestRevertConflicts(projectId, [head.seq])).find(
                    (conflict) => conflict.seq === head.seq,
                )?.reason;
            } catch (ex) {
                // Not knowing does not block: the batch reports a conflict as an outcome
                registerError("BatchRevertModal.check", t("pages.changes.errors.fetchConflicts"), ex);
            }
            if (!closed) {
                setBlocked(reason);
                setChecking(false);
            }
        })();
        return () => {
            closed = true;
        };
    }, []);

    /** Reverts the batch; the server skips the entries of it that cannot be reverted. */
    const revert = async () => {
        setLoading(true);
        try {
            const response = await requestRevertChanges(
                projectId,
                entries.map((entry) => entry.seq),
            );
            await onReverted(response.data.results);
        } catch (ex) {
            displayError(ex, t("pages.changes.errors.revertAll"));
        } finally {
            setLoading(false);
        }
    };

    const skipped = entries.filter((entry) => !hasInverse(entry)).length;

    return (
        <DeleteModal
            data-test-id={"changes-revert-batch-modal"}
            isOpen={true}
            title={title}
            alternativeDeleteButtonText={t("common.action.revert")}
            removeLoading={loading}
            errorMessage={error?.detail}
            // Only the revert waits for the check; the dialog stays closable meanwhile
            deleteDisabled={checking || blocked != null}
            // The dialog's Enter key ignores the disabled button, so it is off while the revert is not offered or under way
            submitOnEnter={!checking && blocked == null && !loading}
            notifications={
                blocked != null && (
                    <Notification data-test-id={"changes-revert-batch-blocked"} intent="warning">
                        {t("pages.changes.revertAll.blocked", { reason: blocked })}
                    </Notification>
                )
            }
            onConfirm={revert}
            onDiscard={onClose}
            render={() => (
                <div>
                    <p>{confirmText}</p>
                    <ul>
                        {entries.filter(hasInverse).map((entry) => (
                            <li key={entry.seq}>{entry.description}</li>
                        ))}
                    </ul>
                    {skipped > 0 && <p>{t("pages.changes.revertAll.skippedNote", { count: skipped })}</p>}
                </div>
            )}
        />
    );
};

export default BatchRevertModal;
