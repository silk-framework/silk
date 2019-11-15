/*
 * This script takes the extracted string outputted by babel-react-intl plugin
 * and generates two files per supported locale. This library tracks translations
 * and makes sure there are no duplicate keys
 */
const manageTranslations = require('react-intl-translations-manager').default;

manageTranslations({
    messagesDirectory: 'build/messages/',
    translationsDirectory: 'src/translations/',
    languages: ['de'], // Any translation --- don't include the default language
    singleMessagesFile: true
});
