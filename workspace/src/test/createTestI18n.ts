import i18next from "i18next";
import type { ReportNamespaces } from "react-i18next";

export const createTestI18n = () => {
    // react-i18next 11 requires this in its types but otherwise adds it only inside useTranslation.
    const namespaces = new Set<string>();
    const reportNamespaces: ReportNamespaces = {
        addUsedNamespaces: (used) => used.flat().forEach((name) => namespaces.add(name)),
        getUsedNamespaces: () => [...namespaces],
    };
    return Object.assign(i18next.createInstance(), { reportNamespaces });
};
