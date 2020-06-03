import React, { useEffect } from "react";
import { commonOp } from "@ducks/common";
import { useDispatch } from "react-redux";
import { useParams } from "react-router";

interface IProps {
    children: JSX.Element[] | JSX.Element;
}
/**
 * AppLayout includes all pages-components and provide
 * the data which based on projectId and taskId
 * @param children
 */
export function AppLayout({ children }: IProps) {
    const dispatch = useDispatch();
    const { projectId, taskId } = useParams();

    useEffect(() => {
        if (projectId) {
            // Fetch the list of projects
            dispatch(commonOp.setProjectId(projectId));
        } else {
            dispatch(commonOp.unsetProject());
        }
        dispatch(commonOp.fetchAvailableDTypesAsync(projectId));

        if (taskId) {
            dispatch(commonOp.setTaskId(taskId));
        } else {
            dispatch(commonOp.unsetTaskId());
        }
    }, [projectId, taskId]);

    return <>{children}</>;
}
