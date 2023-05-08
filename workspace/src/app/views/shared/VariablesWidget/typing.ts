export interface Variable {
    name: string;
    value: string;
    template: string;
    isSensitive: boolean;
    description?: string;
    scope: "project" | "task";
}

export interface VariableWidgetProps {
    projectId: string;
    taskId?: string;
}
