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

    // Fetched on load and whenever the tab comes back into view, so that changes made meanwhile, e.g. by an agent, show up
    React.useEffect(() => {
        if (!projectId) {
            return;
        }
        let stale = false;
        // A failure leaves the widget as it is: the count is a hint, the changes page reports its errors itself
        const fetchSummary = () =>
            requestChangeSummary(projectId)
                .then((response) => {
                    if (!stale) {
                        setUnreviewed(response.data.unreviewed);
                    }
                })
                .catch(() => {});
        const onVisible = () => {
            if (document.visibilityState === "visible") {
                fetchSummary();
            }
        };
        fetchSummary();
        document.addEventListener("visibilitychange", onVisible);
        return () => {
            stale = true;
            document.removeEventListener("visibilitychange", onVisible);
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
