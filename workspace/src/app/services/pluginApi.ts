import { IPlugins } from "../../configs";
import { isReactComponent } from "../utils/reactUtils";

const createPlugin = (options: IPlugins): any => {
    const { entryPoint, name, menuName } = options;
    const { component, route } = entryPoint;

    if (!isReactComponent(component)) {
        console.error("Component property not contain React component");
    }

    if (!component.displayName) {
        component.displayName = name;
    }

    return {
        component,
        path: route,
        menuName,
        isExternal: true,
    };
};

export { createPlugin };
