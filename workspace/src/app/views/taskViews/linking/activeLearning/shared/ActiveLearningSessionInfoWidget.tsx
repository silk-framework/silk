import { LinkingRuleActiveLearningContext } from "../contexts/LinkingRuleActiveLearningContext";
import { ActiveLearningSessionInfo } from "../LinkingRuleActiveLearning.typings";
import React from "react";
import { fetchActiveLearningSessionInfo } from "../LinkingRuleActiveLearning.requests";
import useErrorHandler from "../../../../../hooks/useErrorHandler";
import { useTranslation } from "react-i18next";
import { Notification, Spacing, Spinner, HtmlContentBlock } from "@eccenca/gui-elements";
import { ReferenceLinksStats } from "../../referenceLinks/LinkingRuleReferenceLinks.typing";

interface ActiveLearningSessionInfoWidgetProps {
    activeLearningSessionInfo?: ActiveLearningSessionInfo;
}

/** Displays the active learning session details. */
export const ActiveLearningSessionInfoWidget = ({
    activeLearningSessionInfo,
}: ActiveLearningSessionInfoWidgetProps) => {
    const activeLearningContext = React.useContext(LinkingRuleActiveLearningContext);
    const { sessionInfo, loading } = useActiveLearningSessionInfo(
        activeLearningContext.projectId,
        activeLearningContext.linkingTaskId,
        activeLearningSessionInfo,
    );
    const [t] = useTranslation();

    return loading ? (
        <Spinner />
    ) : sessionInfo ? (
        <HtmlContentBlock>
            {sessionInfo.users.length > 0 ? (
                <>
                    {t("ActiveLearning.statistics.users")}
                    <Spacing />
                    <ul>
                        {sessionInfo.users.map((user) => {
                            return <li key={user.uri}>{user.label || user.uri}</li>;
                        })}
                    </ul>
                    <Spacing />
                </>
            ) : null}
            {sessionInfo.referenceLinks.addedLinks + sessionInfo.referenceLinks.removedLinks > 0 && (
                <>
                    <ReferenceLinksStatsWidget stats={sessionInfo.referenceLinks} />
                </>
            )}
        </HtmlContentBlock>
    ) : (
        <Notification intent="warning">{t("ActiveLearning.statistics.noStats")}</Notification>
    );
};

const ReferenceLinksStatsWidget = ({ stats }: { stats: ReferenceLinksStats }) => {
    const [t] = useTranslation();
    return stats.addedLinks > 0 || stats.removedLinks > 0 ? (
        <>
            {t("ActiveLearning.statistics.referenceLinksChangeStats")}
            <Spacing />
            <ul>
                {stats.addedLinks > 0 ? (
                    <li>
                        {t("common.words.added")}: {stats.addedLinks}
                    </li>
                ) : null}
                {stats.removedLinks > 0 ? (
                    <li>
                        {t("common.words.removed")}: {stats.removedLinks}
                    </li>
                ) : null}
            </ul>
        </>
    ) : null;
};

interface ActiveLearningSessionInfoResult {
    sessionInfo: ActiveLearningSessionInfo | undefined;
    loading: boolean;
}

/** Returns the current active learning session infos. */
export const useActiveLearningSessionInfo = (
    projectId: string,
    linkingTaskId: string,
    activeLearningSessionInfo?: ActiveLearningSessionInfo,
): ActiveLearningSessionInfoResult => {
    const { registerError } = useErrorHandler();
    const [sessionInfo, setSessionInfo] = React.useState<ActiveLearningSessionInfo | undefined>(
        activeLearningSessionInfo,
    );
    const [loading, setLoading] = React.useState(false);
    const [t] = useTranslation();

    React.useEffect(() => {
        if (projectId && linkingTaskId && !sessionInfo) {
            fetchInfos();
        }
    }, [projectId, linkingTaskId]);

    const fetchInfos = async () => {
        setLoading(true);
        try {
            const infos = await fetchActiveLearningSessionInfo(projectId, linkingTaskId);
            setSessionInfo(infos.data);
        } catch (ex) {
            registerError("ActiveLearningSessionInfoWidget.fetchInfos", t("ActiveLearning.statistics.fetchError"), ex);
        } finally {
            setLoading(false);
        }
    };

    return {
        sessionInfo,
        loading,
    };
};
