import React from "react";
import HierarchicalMapping from "./HierarchicalMapping";
import MappingEditorModal from "./MappingEditorModal";

interface IWrapperProps {
    /** Project ID the task is in. */
    projectId: string;
    /** The task the rules are being edited of. */
    taskId: string;
}

const MappingEditorWrapper: React.FC<IWrapperProps> = ({ projectId, taskId }) => {
    const [showMappingEditor, setShowMappingEditor] = React.useState<boolean>(false);
    const [currentRuleId, setCurrentRuleId] = React.useState<string>();
    return (
        <>
            {currentRuleId && (
                <MappingEditorModal
                    projectId={projectId}
                    transformTaskId={taskId}
                    ruleId={currentRuleId}
                    isOpen={showMappingEditor}
                    onClose={() => setShowMappingEditor(false)}
                />
            )}
            <HierarchicalMapping
                project={projectId}
                transformTask={taskId}
                openMappingEditor={(ruleId: string) => {
                    setCurrentRuleId(ruleId);
                    setShowMappingEditor(true);
                }}
                initialRule={"root"}
            />
        </>
    );
};

export default MappingEditorWrapper;
