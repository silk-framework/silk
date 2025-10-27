import { useTranslation } from "react-i18next";
import React from "react";
import useErrorHandler from "../../../../hooks/useErrorHandler";
import { addPathToReferenceEntitiesCache } from "../../linking/LinkingRuleEditor.requests";
import { Button, Notification, SimpleDialog, Markdown } from "@eccenca/gui-elements";

interface PathNotInCacheModalProps {
    projectId: string;
    linkingTaskId: string;
    /** The path that should be added to the cache. */
    path: string;
    /** true when the path should be added to the target input cache. */
    toTarget: boolean;
    /** Executed when the path was added successfully. */
    onAddPath: () => any;
    /** Executed on close. */
    onClose: () => any;
}

/** Shown when a path is missing in the entity cache and the evaluation cannot be run. */
export const PathNotInCacheModal = ({
    projectId,
    linkingTaskId,
    toTarget,
    path,
    onAddPath,
    onClose,
}: PathNotInCacheModalProps) => {
    const [t] = useTranslation();
    const [loading, setLoading] = React.useState(false);
    const [error, setError] = React.useState<string | undefined>(undefined);
    const { registerErrorI18N } = useErrorHandler();

    const addPath = async () => {
        setLoading(true);
        try {
            await addPathToReferenceEntitiesCache(projectId, linkingTaskId, path, toTarget);
            onAddPath();
        } catch (ex) {
            if (ex.isFetchError) {
                setError(ex.asString());
            } else {
                registerErrorI18N("RuleEditor.evaluation.PathNotInCacheModal.pathCouldNotBeAdded", ex);
            }
        } finally {
            setLoading(false);
        }
    };
    const notification = error ? <Notification intent="warning" message={error} /> : undefined;

    return (
        <SimpleDialog
            data-test-id={"pathNotInCacheModal"}
            isOpen={true}
            title={t("RuleEditor.evaluation.PathNotInCacheModal.missingPath")}
            onClose={onClose}
            notifications={notification}
            size={"small"}
            intent={"warning"}
            actions={[
                <Button
                    data-test-id={"loadAndReEvaluateBtn"}
                    type={"submit"}
                    intent="primary"
                    text={t("RuleEditor.evaluation.PathNotInCacheModal.loadAndReEvaluate")}
                    onClick={addPath}
                    loading={loading}
                />,
                <Button text={t("common.action.cancel")} onClick={onClose} />,
            ]}
        >
            <Markdown>
                {`${t("RuleEditor.evaluation.PathNotInCacheModal.messagePartA")}\n\n- ${path}\n\n${t(
                    "RuleEditor.evaluation.PathNotInCacheModal.messagePartB",
                )}`}
            </Markdown>
        </SimpleDialog>
    );
};
