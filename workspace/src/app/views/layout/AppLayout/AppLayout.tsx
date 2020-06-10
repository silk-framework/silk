import React from "react";

interface IProps {
    children: JSX.Element[] | JSX.Element;
}
/**
 * AppLayout includes all pages-components and provide
 * the data which based on projectId and taskId
 * @param children
 */
export function AppLayout({ children }: IProps) {
    return <>{children}</>;
}
