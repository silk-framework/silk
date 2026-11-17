import React from "react";
import { useTranslation } from "react-i18next";
import { Button, HtmlContentBlock, Notification, SimpleDialog } from "@eccenca/gui-elements";
import Loading from "../../shared/Loading";
import useErrorHandler from "../../../hooks/useErrorHandler";
import { IChangeEntry, searchChanges, STATE_FACET, UNREVIEWED_STATE } from "./changesRequests";

interface IProps {
    projectId: string;
    upTo: number;
    all: boolean;
    loading: boolean;
    onConfirm: () => void;
    onClose: () => void;
}

/** Lists every pending change covered by the captured watermark, independently of pagination and search filters. */
const MarkReviewedModal = ({ projectId, upTo, all, loading, onConfirm, onClose }: IProps) => {
    const [t] = useTranslation();
    const { registerError } = useErrorHandler();
    const [entries, setEntries] = React.useState<IChangeEntry[] | undefined>(undefined);
    const [failed, setFailed] = React.useState(false);
    const [revision, setRevision] = React.useState(0);

    React.useEffect(() => {
        let closed = false;
        setEntries(undefined);
        setFailed(false);
        (async () => {
            try {
                const response = await searchChanges(projectId, {
                    facets: [{ facetId: STATE_FACET, type: "keyword", keywordIds: [UNREVIEWED_STATE] }],
                    limit: 0,
                });
                if (!closed) {
                    // A change that arrived after the page loaded must not enter this approval.
                    setEntries(response.data.results.filter((entry) => entry.seq <= upTo));
                }
            } catch (ex) {
                if (!closed) {
                    setFailed(true);
                    registerError("MarkReviewedModal.load", t("pages.changes.errors.fetchChanges"), ex);
                }
            }
        })();
        return () => {
            closed = true;
        };
    }, [projectId, upTo, revision]);

    return (
        <SimpleDialog
            data-test-id={"changes-mark-reviewed-modal"}
            size="large"
            title={t("pages.changes.markReviewed.title")}
            isOpen={true}
            preventSimpleClosing={loading}
            onClose={() => {
                if (!loading) {
                    onClose();
                }
            }}
            actions={[
                <Button
                    key="confirm"
                    affirmative
                    loading={loading}
                    disabled={!entries?.length || loading}
                    onClick={() => {
                        if (entries?.length && !loading) {
                            onConfirm();
                        }
                    }}
                    data-test-id={"changes-mark-reviewed-confirm-btn"}
                >
                    {t("common.action.confirm")}
                </Button>,
                <Button key="cancel" onClick={onClose} disabled={loading}>
                    {t("common.action.cancel")}
                </Button>,
            ]}
        >
            <HtmlContentBlock>
                <p>
                    {all
                        ? t("pages.changes.markReviewed.confirmText")
                        : t("pages.changes.markReviewed.confirmUpToText", { seq: upTo })}
                </p>
                {failed ? (
                    <Notification
                        intent="danger"
                        message={t("pages.changes.errors.fetchChanges")}
                        actions={
                            <Button text={t("common.action.retry")} onClick={() => setRevision((value) => value + 1)} />
                        }
                    />
                ) : entries ? (
                    <>
                        <p>{t("pages.changes.markReviewed.affectedCount", { count: entries.length })}</p>
                        <ul>
                            {entries.map((entry) => (
                                <li key={entry.seq}>{entry.description}</li>
                            ))}
                        </ul>
                        {entries.length === 0 && <p>{t("pages.changes.markReviewed.nothing")}</p>}
                    </>
                ) : (
                    <Loading posInline delay={0} description={t("pages.changes.loading")} />
                )}
            </HtmlContentBlock>
        </SimpleDialog>
    );
};

export default MarkReviewedModal;
