import { fetchProjectAccessControl, fetchUserData, AccessControlConfig } from "@ducks/workspace/requests";
import React, { useState } from "react";
import { useSelector } from "react-redux";
import { commonSel } from "@ducks/common";
import { pluginRegistry, SUPPORTED_PLUGINS } from "../views/plugins/PluginRegistry";
import { ProjectAccessControlManagementProps } from "../views/plugins/plugin.types";
import useErrorHandler from "./useErrorHandler";

/** Convenience hook for ACL management. Either projectId and computeInitialAcl */
interface UseAclManagementComponentProps {
    projectId?: string;
    /** Called when the groups change. */
    onChange: (aclData: AccessControlConfig) => void;
    /** Optional error handler. If not set, the error is output to the global error queue. */
    errorHandler?: (error) => void;
    /** If set, this is used as the initial value.*/
    externalInitialAclGroups?: AccessControlConfig;
    /** Receives the current user and project ACLs and returns a custom ACL that should be used as initial value.
     * This is not executed if externalInitialAclGroups is defined. */
    computeInitialAcl?: (userAcl: AccessControlConfig, projectAcl: AccessControlConfig) => Promise<AccessControlConfig>;
}

interface UseAclManagementComponentReturnProps {
    /** The current component that should be rendered. */
    component: React.JSX.Element | null;
    /** True when relevant data is fetched from the backend. */
    loading: boolean;
    /** If access control is enabled. */
    enabled: boolean;
}

/** Returns the ACL management component if ACL is enabled.
 * Either projectId for setting the initial ACL from the project or externalInitialAclGroups must be defined. */
export const useProjectAclManagementComponent = ({
    projectId,
    errorHandler,
    externalInitialAclGroups,
    onChange,
    computeInitialAcl = (_userAcl, projectAcl) => Promise.resolve(projectAcl),
}: UseAclManagementComponentProps): UseAclManagementComponentReturnProps => {
    const initialSettings = useSelector(commonSel.initialSettingsSelector);
    const aclEnabled = initialSettings?.aclEnabled ?? false;
    const [initialAcl, setInitialAcl] = useState<AccessControlConfig | undefined>(externalInitialAclGroups);
    const projectAclManagement = pluginRegistry.pluginReactComponent<ProjectAccessControlManagementProps>(
        SUPPORTED_PLUGINS.DI_PROJECT_ACL_MANAGEMENT,
    );
    const [loading, setLoading] = React.useState<boolean>(false);
    const { registerErrorI18N } = useErrorHandler();

    const fetchAclData = React.useCallback(async () => {
        if (projectId && aclEnabled && !externalInitialAclGroups) {
            setLoading(true);
            try {
                const [projectAcl, userAcl] = await Promise.all([
                    fetchProjectAccessControl(projectId),
                    fetchUserData(),
                ]);
                const computedInitialAcl = await computeInitialAcl(userAcl.data, projectAcl.data);
                setInitialAcl(computedInitialAcl);
                onChange(computedInitialAcl);
            } catch (error) {
                errorHandler ? errorHandler(error) : registerErrorI18N("", error);
            } finally {
                setLoading(false);
            }
        }
    }, [aclEnabled, projectId]);

    React.useEffect(() => {
        void fetchAclData();
    }, [fetchAclData]);

    if (!aclEnabled || !(projectId || externalInitialAclGroups) || !projectAclManagement) {
        return {
            component: null,
            loading: false,
            enabled: aclEnabled,
        };
    }

    return {
        loading,
        component: loading ? null : (
            <projectAclManagement.Component projectId={projectId} initialGroups={initialAcl} onChange={onChange} />
        ),
        enabled: aclEnabled,
    };
};
