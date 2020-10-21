import i18n from "i18next";
import { initReactI18next } from "react-i18next";
import LanguageDetector from "i18next-browser-languagedetector";
import en from "./locales/en.json";
import de from "./locales/de.json";
import { isTestEnv, isDevelopment } from "./app/constants/path";
import Store from "store";

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
        lng: Store.get("locale"),
    });

export default i18n;
