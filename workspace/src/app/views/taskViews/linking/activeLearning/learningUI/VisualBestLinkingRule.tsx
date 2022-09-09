import { LinkingRuleEditor, LinkingRuleEditorOptionalContext } from "../../LinkingRuleEditor";
import React from "react";
import { ILinkingRule, OptionallyLabelledParameter } from "../../linking.types";
import { LinkingRuleActiveLearningContext } from "../contexts/LinkingRuleActiveLearningContext";

interface Props {
    rule: OptionallyLabelledParameter<ILinkingRule>;
}

/** Read-only visual linking rule representation. */
export const VisualBestLinkingRule = ({ rule }: Props) => {
    const activeLearningContext = React.useContext(LinkingRuleActiveLearningContext);
    const [show, setShow] = React.useState(true);

    React.useEffect(() => {
        setShow(false);
    }, [rule]);

    React.useEffect(() => {
        if (!show) {
            setShow(true);
        }
    }, [show]);

    return activeLearningContext.linkTask ? (
        <LinkingRuleEditorOptionalContext.Provider
            value={{
                linkingRule: {
                    ...activeLearningContext.linkTask,
                    parameters: {
                        ...activeLearningContext.linkTask.parameters,
                        rule: rule,
                    },
                },
                showRuleOnly: true,
                hideMinimap: true,
                zoomRange: [0.1, 1],
            }}
        >
            <div style={{ position: "relative", height: "400px" }}>
                {show ? (
                    <LinkingRuleEditor
                        projectId={activeLearningContext.projectId}
                        linkingTaskId={activeLearningContext.linkingTaskId}
                        instanceId={"best-learned-rule"}
                    />
                ) : null}
            </div>
        </LinkingRuleEditorOptionalContext.Provider>
    ) : null;
};
