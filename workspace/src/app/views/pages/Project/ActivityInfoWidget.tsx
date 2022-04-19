import React from "react";
import {
    Card,
    IconButton,
    Spacing,
    OverviewItem,
    OverviewItemDescription,
    OverviewItemActions,
    OverviewItemLine,
} from "gui-elements";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";
import { commonSel } from "@ducks/common";
import { routerOp } from "@ducks/router";
import { SERVE_PATH } from "../../../constants/path";

const ActivityInfoWidget = () => {
    const projectId = useSelector(commonSel.currentProjectIdSelector);
    const dispatch = useDispatch();
    const [t] = useTranslation();

    const projectPath = `projects/${projectId}/activities?page=1&limit=25`;
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
                        onClick={() => dispatch(routerOp.goToPage(projectPath))}
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
