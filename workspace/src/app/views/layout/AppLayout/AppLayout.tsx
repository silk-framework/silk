import React, { useEffect } from "react";
import { commonOp, commonSel } from "@ducks/common";
import { useDispatch, useSelector } from "react-redux";
import { useParams } from "react-router";

/**
 * AppLayout includes all pages-components and provide
 * the data which based on projectId and taskId
 * @param children
 */
export function AppLayout({ children }) {
    const dispatch = useDispatch();
    const { projectId, taskId } = useParams();

    const currentProjectId = useSelector(commonSel.currentProjectIdSelector);
    const currentTaskId = useSelector(commonSel.currentTaskIdSelector);

    useEffect(() => {
        if (projectId) {
            // Fetch the list of projects
            dispatch(commonOp.setProjectId(projectId));
            dispatch(commonOp.fetchAvailableDTypesAsync(projectId));
        } else if (currentProjectId) {
            dispatch(commonOp.unsetProject());
        }

        if (taskId) {
            dispatch(commonOp.setTaskId(taskId));
        } else if (currentTaskId) {
            dispatch(commonOp.unsetTaskId());
        }
    }, [projectId, taskId]);

    return { children };
}
