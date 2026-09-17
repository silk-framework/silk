import React from "react";
import { renderHook } from "@testing-library/react";
import { FileUploadError, FileUploadRestriction } from "@eccenca/gui-elements";
import i18next from "i18next";
import { I18nextProvider } from "react-i18next";
import translations from "../../../../locales/manual/en.json";
import { useFileUploadLabels } from "./useFileUploadLabels";

const i18n = i18next.createInstance();
void i18n.init({
    lng: "en",
    resources: { en: { translation: translations } },
    initImmediate: false,
    interpolation: { escapeValue: false },
});
const wrapper = ({ children }: { children: React.ReactNode }) => (
    <I18nextProvider i18n={i18n}>{children}</I18nextProvider>
);

const cases: Array<[FileUploadRestriction, string]> = [
    [{ code: "maxFileSize", maxFileSize: 12 }, "data.ttl exceeds the maximum file size of 12 bytes."],
    [
        { code: "fileType", acceptedFileTypes: [".ttl", "text/*"] },
        "data.ttl is not an accepted file type. Allowed: .ttl, text/*.",
    ],
    [{ code: "maxNumberOfFiles", maxNumberOfFiles: 2 }, "Select at most 2 incomplete files at a time."],
    [{ code: "duplicate" }, "data.ttl is already selected."],
    [{ code: "unknown" }, "The selected file or files cannot be added."],
];

it.each(cases)("formats restriction %j without depending on diagnostic text", (restriction, expected) => {
    const { result } = renderHook(useFileUploadLabels, { wrapper });
    const error: FileUploadError = {
        kind: "restriction",
        restriction,
        error: new Error("untranslated diagnostic"),
        file: { id: "file", name: "data.ttl" },
    };
    expect(result.current.formatError(error)).toBe(expected);
});
