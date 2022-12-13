import i18n from "i18next";
import LanguageDetector from "i18next-browser-languagedetector";
import { initReactI18next } from "react-i18next";
import Store from "store";

import { isDevelopment, isTestEnv } from "./app/constants/path";
import de from "./locales/generated/de.json";
import en from "./locales/generated/en.json";
import fr from "./locales/generated/fr.json";

// Fetches the configured language from local storage
export const fetchStoredLang: () => string = () => {
    return Store.get("i18nextLng", "en");
};

// Sets the language in local storage
export const setStoredLang: (lang: string) => void = (lang) => {
    Store.set("i18nUserChoice", true);
};

i18n.use(initReactI18next)
    .use(LanguageDetector)
    .init({
        detection: {
            lookupQuerystring: "lng",
        },
        resources: {
            de: { translation: de },
            en: { translation: en },
            fr: { translation: fr },
        },
        debug: isDevelopment && !isTestEnv,
        interpolation: { escapeValue: false },
        fallbackLng: "en",
        lng: fetchStoredLang(),
    });

export default i18n;
