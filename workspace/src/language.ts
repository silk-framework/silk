import i18n from "i18next";
import { initReactI18next } from "react-i18next";
import LanguageDetector from "i18next-browser-languagedetector";
import en from "./locales/en.json";
import de from "./locales/de.json";
import { isDevelopment, isTestEnv } from "./app/constants/path";
import Store from "store";
import { DEFAULT_LANG } from "./app/constants/base";

i18n.use(initReactI18next)
    .use(LanguageDetector)
    .init({
        resources: {
            en: { translation: en },
            de: { translation: de },
        },
        debug: isDevelopment && !isTestEnv,
        interpolation: { escapeValue: false },
        lng: Store.get("locale") || DEFAULT_LANG,
    });

export default i18n;
