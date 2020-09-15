import i18n from "i18next";
import { initReactI18next } from "react-i18next";
import LanguageDetector from "i18next-browser-languagedetector";
import en from "./locales/en.json";
import de from "./locales/de.json";
import { isDevelopment } from "./app/constants/path";
import { DEFAULT_LANG } from "./app/constants/base";

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
        debug: isDevelopment,
        interpolation: {},
        fallbackLng: DEFAULT_LANG,
    });

export default i18n;
