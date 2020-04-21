import i18n from "i18next";
import { initReactI18next } from "react-i18next";
import LanguageDetector from "i18next-browser-languagedetector";
import en from "./translations/en.json";
import de from "./translations/de.json";

i18n
    .use(initReactI18next)
    .use(LanguageDetector)
    .init({
        resources: {
            en: {
                translation: {
                    // general
                    languageIntroduction: `This site has multi language support.
                    The following languages are supported: '{{languages}}'.
                    The currently choosen is '{{language}}'.
                    The Fallback language is '{{fallbackLanguage}}'.`,
                    switchLanguage: 'Switch language',
                    // standard button label. the text should be provided by gui-elements
                    cancel: 'Cancel',
                    ...en,
                }
            },
            de: {
                translation: {
                    // general
                    languageIntroduction: `Diese Seite ist in mehreren Sprachen verfügrbar.
                    Die folgenden Sprachen existieren: '{{languages}}'.
                    Die aktuell ausgewählte ist '{{language}}'.
                    Die Ersatzsprache ist '{{fallbackLanguage}}'.`,
                    switchLanguage: 'Sprache wechseln',
                    // standard button label. the text should be provided by gui-elements
                    cancel: 'Abbrechen',
                    ...de,
                }
            },
        },
        fallbackLng: "en",
        interpolation: {
            escapeValue: false
        }
    });
