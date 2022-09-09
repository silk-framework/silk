import React from "react";
import { useParams } from "react-router";
import NotFound from "../../../views/pages/NotFound";
import { TransformRuleEditor } from "./TransformRuleEditor";

const TransformEditorPage = () => {
    const { projectId, ruleId, transformTaskId } = useParams<any>();
    return !projectId || !ruleId || !transformTaskId ? (
        <NotFound />
    ) : (
        <TransformRuleEditor
            projectId={projectId}
            ruleId={ruleId}
            transformTaskId={transformTaskId}
            instanceId={"tab-instance"}
        />
    );
};

export default TransformEditorPage;
