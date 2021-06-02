import React from "react";

export interface IWorkflowEditorProps {
    baseUrl: string
    projectId: string
    workflowTaskId: string
}
export function WorkflowEditor({baseUrl, projectId, workflowTaskId}: IWorkflowEditorProps) {
    return <div>New editor</div>
}