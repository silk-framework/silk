import React from "react";
import { Card, CardHeader, CardTitle, CardOptions, IconButton } from "@eccenca/gui-elements";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";
import { commonSel } from "@ducks/common";
import { routerOp } from "@ducks/router";
import { SERVE_PATH } from "../../../constants/path";
import { AppDispatch } from "store/configureStore";
import { requestChangeSummary } from "../ProjectChanges/changesRequests";

/** Links to the change journal of the project; the title counts the changes awaiting review, if any. */
const ChangesWidget = () => {
    const projectId = useSelector(commonSel.currentProjectIdSelector);
    const dispatch = useDispatch<AppDispatch>();
    const [t] = useTranslation();
    const [unreviewed, setUnreviewed] = React.useState<number>(0);

    // Fetched on load and whenever the user comes back, so that changes made meanwhile, e.g. by an agent, show up:
    // the tab shown again (visibilitychange), or the window focused again from another application (focus)
    React.useEffect(() => {
        setUnreviewed(0); // not the count of the previous project while this one loads, nor if it fails
        if (!projectId) {
            return;
        }
        let stale = false;
        let inFlight = false;
        // A failure leaves the count at zero: it is a hint, the changes page reports its errors itself
        const fetchSummary = () => {
            inFlight = true;
            requestChangeSummary(projectId)
                .then((response) => {
                    if (!stale) {
                        setUnreviewed(response.data.unreviewed);
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
    return (
        <Card>
            <CardHeader>
                <CardTitle>
                    <h2>
                        {unreviewed > 0
                            ? t("widget.ChangesWidget.titleWithUnreviewed", { count: unreviewed })
                            : t("widget.ChangesWidget.title", "Changes")}
                    </h2>
                </CardTitle>
                <CardOptions>
                    <IconButton
                        onClick={(e) => {
                            e.preventDefault();
                            e.stopPropagation();
                            dispatch(routerOp.goToPage(changesPath));
                        }}
                        href={`${SERVE_PATH}/${changesPath}`}
                        data-test-id={"open-project-changes-btn"}
                        name="item-viewdetails"
                        text={t("widget.ChangesWidget.view", "View project changes")}
                    />
                </CardOptions>
            </CardHeader>
        </Card>
    );
};

export default ChangesWidget;
