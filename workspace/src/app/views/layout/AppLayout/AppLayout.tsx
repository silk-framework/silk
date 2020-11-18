import React from "react";

interface IProps {
    children: React.ReactNode;
}
/**
 * AppLayout includes all pages-components and provide
 * the data which based on projectId and taskId
 * @param children
 */
export function AppLayout({ children }: IProps) {
    return <>{children}</>;
}
