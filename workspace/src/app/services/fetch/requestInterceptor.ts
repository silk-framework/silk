import { AxiosRequestConfig } from "axios";
import { is } from "ramda";

export const requestInterceptor = (config: AxiosRequestConfig) => {
    const cfg = {
        ...config,
    };

    if (config.headers["Content-Type"] === "application/x-www-form-urlencoded" && is(Object, config.data)) {
        const { data } = config;

        const serializedData: string[] = [];
        for (const key in data) {
            serializedData.push(key + "=" + encodeURIComponent(data[key]));
        }
        cfg.data = serializedData.join("&");
    }

    return cfg;
};
