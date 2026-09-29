import i18n from "i18next";
import { initReactI18next } from "react-i18next";
import LanguageDetector from "i18next-browser-languagedetector";
import de from "./locales/generated/de.json";
import en from "./locales/generated/en.json";
import fr from "./locales/generated/fr.json";
import { isDevelopment, isTestEnv } from "./app/constants/path";

const USER_LANGUAGE_CHOICE_KEY = "i18nUserChoice";
const I18NEXT_LANGUAGE_KEY = "i18nextLng";
const LANGUAGE_QUERY_PARAMETER = "lng";
const SUPPORTED_LANGUAGES = ["de", "en", "fr"] as const;

export const fetchRequestedLanguage = (): string | undefined => {
    const requestedLanguage = new URLSearchParams(window.location.search).get(LANGUAGE_QUERY_PARAMETER);
    const baseLanguage = requestedLanguage?.split(/[-_]/, 1)[0].toLowerCase();
    return SUPPORTED_LANGUAGES.find((language) => language === baseLanguage);
};

export const fetchUserSelectedLanguage = (): string | undefined => {
    try {
        if (localStorage.getItem(USER_LANGUAGE_CHOICE_KEY) !== "true") {
            return undefined;
        }
        const storedLanguage = localStorage.getItem(I18NEXT_LANGUAGE_KEY);
        if (!storedLanguage) {
            return undefined;
        }
        // Earlier versions of DI stored this value as a JSON string through store.js.
        try {
            const parsedLanguage: unknown = JSON.parse(storedLanguage);
            return typeof parsedLanguage === "string" ? parsedLanguage : undefined;
        } catch {
            return storedLanguage;
        }
    } catch {
        return undefined;
    }
};

// i18next-browser-languagedetector stores i18nextLng when the language changes.
export const markLanguageAsUserChoice = (): void => {
    try {
        localStorage.setItem(USER_LANGUAGE_CHOICE_KEY, "true");
    } catch {
        // Language changes still work when browser storage is unavailable.
    }
};

const initialUserSelectedLanguage = fetchUserSelectedLanguage();

i18n.use(initReactI18next)
    .use(LanguageDetector)
    .init({
        detection: {
            lookupQuerystring: LANGUAGE_QUERY_PARAMETER,
        },
        resources: {
            de: { translation: de },
            en: { translation: en },
            fr: { translation: fr },
        },
        debug: isDevelopment && !isTestEnv,
        interpolation: { escapeValue: false },
        fallbackLng: "en",
        supportedLngs: SUPPORTED_LANGUAGES,
        ...(initialUserSelectedLanguage ? { lng: initialUserSelectedLanguage } : {}),
    });

export default i18n;
