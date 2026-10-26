import React from "react";
import {
    Button,
    Card,
    CardContent,
    CardHeader,
    CardOptions,
    CardTitle,
    Divider,
    IconButton,
    Notification,
} from "@eccenca/gui-elements";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";
import { commonSel } from "@ducks/common";
import { routerOp } from "@ducks/router";
import { SERVE_PATH } from "../../../constants/path";
import { AppDispatch } from "store/configureStore";
import { IChangeSummary, requestChangeSummary } from "../ProjectChanges/changesRequests";

/** Links to the change journal of the project and tells its state: the changes awaiting review, or what has been recorded. */
const ChangesWidget = () => {
    const projectId = useSelector(commonSel.currentProjectIdSelector);
    const dispatch = useDispatch<AppDispatch>();
    const [t] = useTranslation();
    // The journal's numbers; unknown while they load, or if the request failed
    const [summary, setSummary] = React.useState<IChangeSummary | undefined>(undefined);

    // Fetched on load and whenever the user comes back, so that changes made meanwhile, e.g. by an agent, show up:
    // the tab shown again (visibilitychange), or the window focused again from another application (focus)
    React.useEffect(() => {
        setSummary(undefined); // not the numbers of the previous project while this one loads, nor if it fails
        if (!projectId) {
            return;
        }
        let stale = false;
        let inFlight = false;
        // A failure leaves the numbers unknown: they are a hint, the changes page reports its errors itself
        const fetchSummary = () => {
            inFlight = true;
            requestChangeSummary(projectId)
                .then((response) => {
                    if (!stale) {
                        setSummary(response.data);
                    }
                })
                .catch(() => {})
                .finally(() => {
                    inFlight = false;
                });
        };
        // Showing a hidden tab fires both events, so a fetch already under way is not repeated
        const onReturn = () => {
            if (document.visibilityState === "visible" && !inFlight) {
                fetchSummary();
            }
        };
        fetchSummary();
        document.addEventListener("visibilitychange", onReturn);
        window.addEventListener("focus", onReturn);
        return () => {
            stale = true;
            document.removeEventListener("visibilitychange", onReturn);
            window.removeEventListener("focus", onReturn);
        };
    }, [projectId]);

    const changesPath = `projects/${projectId}/changes`;
    const changesUrl = `${SERVE_PATH}/${changesPath}`;
    const openChanges = (e: React.MouseEvent) => {
        e.preventDefault();
        e.stopPropagation();
        dispatch(routerOp.goToPage(changesPath));
    };

    /** The state of the journal in one line: the changes awaiting review with the way to them, else what has been recorded. */
    const status = (summary: IChangeSummary): React.ReactNode => {
        // Changes the cap dropped unreviewed wait for the review as well: until it, the watermark stays put
        if (summary.unreviewed > 0 || summary.droppedUnreviewed > 0) {
            return (
                <Notification
                    data-test-id={"project-changes-unreviewed"}
                    intent="warning"
                    message={
                        <>
                            {summary.unreviewed > 0 && (
                                <div>{t("widget.ChangesWidget.unreviewed", { count: summary.unreviewed })}</div>
                            )}
                            {summary.droppedUnreviewed > 0 && (
                                <div>{t("widget.ChangesWidget.dropped", { count: summary.droppedUnreviewed })}</div>
                            )}
                        </>
                    }
                    actions={
                        <Button
                            data-test-id={"review-project-changes-btn"}
                            href={changesUrl}
                            onClick={openChanges}
                            text={t("widget.ChangesWidget.review", "Review changes")}
                        />
                    }
                />
            );
        } else if (summary.latestSeq > 0) {
            return t("widget.ChangesWidget.allReviewed", { count: summary.latestSeq });
        } else {
            return t("widget.ChangesWidget.noChanges", "No changes recorded yet.");
        }
    };

    return (
        <Card>
            <CardHeader>
                <CardTitle>
                    <h2>{t("widget.ChangesWidget.title", "Changes")}</h2>
                </CardTitle>
                <CardOptions>
                    <IconButton
                        onClick={openChanges}
                        href={changesUrl}
                        data-test-id={"open-project-changes-btn"}
                        name="item-viewdetails"
                        text={t("widget.ChangesWidget.view", "View project changes")}
                    />
                </CardOptions>
            </CardHeader>
            {summary && (
                <>
                    <Divider />
                    <CardContent data-test-id={"project-changes-status"}>{status(summary)}</CardContent>
                </>
            )}
        </Card>
    );
};

export default ChangesWidget;
