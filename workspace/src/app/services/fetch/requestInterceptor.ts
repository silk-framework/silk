import { AxiosRequestConfig } from "axios";

export const requestInterceptor = (config: AxiosRequestConfig) => {
    const cfg = {
        ...config,
    };

    if (
        config.headers &&
        config.headers["Content-Type"] === "application/x-www-form-urlencoded" &&
        typeof config.data === "object"
    ) {
        const { data } = config;

        const serializedData: string[] = [];
        for (const key in data) {
            serializedData.push(key + "=" + encodeURIComponent(data[key]));
        }
        cfg.data = serializedData.join("&");
    }

    return cfg;
};
