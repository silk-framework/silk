import i18n from "i18next";
import { initReactI18next } from "react-i18next";
import LanguageDetector from "i18next-browser-languagedetector";
import en from "./locales/en.json";
import de from "./locales/de.json";
import { isDevelopment, isTestEnv } from "./app/constants/path";
import Store from "store";

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
            en: { translation: en },
            de: { translation: de },
        },
        debug: isDevelopment && !isTestEnv,
        interpolation: { escapeValue: false },
        lng: fetchStoredLang(),
    });

export default i18n;
