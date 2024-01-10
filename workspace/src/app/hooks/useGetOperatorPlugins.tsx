import { requestRuleOperatorPluginsDetails } from "@ducks/common/requests";
import { IPluginDetails } from "@ducks/common/typings";
import React from "react";

//get rule operator plugins with optional params to get  input operators only
export const useGetRuleOperatorPlugins = (inputOperatorsOnly = false) => {
    const [plugins, setPlugins] = React.useState<Record<string, IPluginDetails>>();

    React.useEffect(() => {
        (async () => {
            setPlugins((await requestRuleOperatorPluginsDetails(inputOperatorsOnly)).data);
        })();
    }, []);

    const getPluginDetail = React.useCallback(
        (operatorId: string) => {
            if (!plugins) return;
            const pluginsMap = new Map(Object.entries(plugins));
            return pluginsMap.get(operatorId);
        },
        [plugins]
    );

    const getPluginDetailLabel = (operatorId: string) => getPluginDetail(operatorId)?.title ?? operatorId;

    return { plugins, getPluginDetail, getPluginDetailLabel };
};
