import { onErrorHandler } from "./app/services/errorLogger";
import { createPlugin } from "./app/services/pluginApi";

/**
 * @override
 */
const registerGlobalListeners = () => {
    window.onerror = onErrorHandler;
    (<any>window).DMInstance = {
        createPlugin
    };
};

export default registerGlobalListeners;
