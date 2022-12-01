import { commonSel } from "@ducks/common";
import { routerOp } from "@ducks/router";
import {
    Card,
    IconButton,
    OverviewItem,
    OverviewItemActions,
    OverviewItemDescription,
    OverviewItemLine,
    Spacing,
} from "@eccenca/gui-elements";
import React from "react";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";

import { SERVE_PATH } from "../../../constants/path";

const ActivityInfoWidget = () => {
    const projectId = useSelector(commonSel.currentProjectIdSelector);
    const dispatch = useDispatch();
    const [t] = useTranslation();

    const projectPath = `projects/${projectId}/activities?page=1&limit=25&sortBy=recentlyUpdated&sortOrder=ASC`;
    return (
        <Card>
            <OverviewItem hasSpacing>
                <OverviewItemDescription>
                    <OverviewItemLine>
                        <Spacing vertical size="small" />
                        <strong>
                            <h2>{t("widget.ActivityInfoWidget.title", "Activities")} </h2>
                        </strong>
                    </OverviewItemLine>
                </OverviewItemDescription>
                <OverviewItemActions>
                    <IconButton
                        onClick={(e) => {
                            e.preventDefault();
                            e.stopPropagation();
                            dispatch(routerOp.goToPage(projectPath));
                        }}
                        href={`${SERVE_PATH}/${projectPath}`}
                        data-test-id={"open-project-activities-btn"}
                        name="item-viewdetails"
                        text={t("widget.ActivityInfoWidget.view", "View project activities")}
                    />
                    <Spacing vertical size="small" />
                </OverviewItemActions>
            </OverviewItem>
        </Card>
    );
};

export default ActivityInfoWidget;
