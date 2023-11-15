// see docs here https://jinja.palletsprojects.com/en/3.1.x/ for jinja convention
export interface Variable {
    name: string;
    value: string | null;
    template: string | null;
    isSensitive: boolean;
    description?: string;
    scope: "project" | "task";
}

export interface VariableWidgetProps {
    projectId: string;
    taskId?: string;
}

export interface VariableDependencies {
    dependentVariables: string[];
    dependentTasks: {
        id: string;
        label: string;
        taskLink: string;
    }[];
}
