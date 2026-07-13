import { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { commonSel } from "@ducks/common";
import { requestTaskData } from "@ducks/shared/requests";
import { requestArtefactProperties } from "@ducks/common/requests";
import { IPluginDetails } from "@ducks/common/typings";
import { commonSlice } from "@ducks/common/commonSlice";
import useErrorHandler from "../../../hooks/useErrorHandler";

/** Fetches the plugin details of a project task, using the redux artefact properties cache.
 * Pages use this for main-content decisions (e.g. preview type, header icon), independently
 * of the `TaskConfig` widget that is only mounted while the side widgets rail is expanded. */
export const useTaskPluginDetails = (projectId?: string, taskId?: string): IPluginDetails | undefined => {
    const { registerError } = useErrorHandler();
    const dispatch = useDispatch();
    const { cachedArtefactProperties } = useSelector(commonSel.artefactModalSelector);
    const [pluginDetails, setPluginDetails] = useState<IPluginDetails | undefined>(undefined);

    useEffect(() => {
        if (!projectId || !taskId) {
            return;
        }
        let cancelled = false;
        (async () => {
            try {
                const taskData = (await requestTaskData(projectId, taskId, true)).data;
                const artefactId = taskData.data.type;
                if (!artefactId) {
                    return;
                }
                let details = cachedArtefactProperties[artefactId];
                if (!details) {
                    details = await requestArtefactProperties(artefactId);
                    dispatch(commonSlice.actions.setCachedArtefactProperty(details));
                }
                if (!cancelled) {
                    setPluginDetails(details);
                }
            } catch (ex) {
                registerError("useTaskPluginDetails", "Failed to load task plugin details.", ex);
            }
        })();
        return () => {
            cancelled = true;
        };
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [projectId, taskId]);

    return pluginDetails;
};
