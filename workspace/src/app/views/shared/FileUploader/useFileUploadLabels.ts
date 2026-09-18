import { FileUploadLabels } from "@eccenca/gui-elements";
import { useTranslation } from "react-i18next";

/** Workspace translations for the shared upload component. */
export const useFileUploadLabels = (): FileUploadLabels => {
    const [t] = useTranslation();
    return {
        cancelFile: t("FileUploader.cancelUpload"),
        continueUpload: t("FileUploader.continueUpload"),
        browse: t("FileUploader.browse"),
        completedFiles: (completed, total) => t("FileUploader.completedFiles", { completed, total }),
        dropHereOr: t("FileUploader.dropHereOr"),
        fileUploadProgress: (file) => t("FileUploader.fileUploadProgress", { fileName: file.name }),
        overallUploadProgress: t("FileUploader.overallUploadProgress"),
        retry: t("FileUploader.retry"),
        removeFile: t("FileUploader.removeFile"),
        removedFile: (file) => t("FileUploader.removedFile", { fileName: file.name }),
        selectedFile: (file) => t("FileUploader.selectedFile", { fileName: file.name }),
        stopUploads: t("FileUploader.stopUploads"),
        uploadCancelled: t("FileUploader.uploadCancelled"),
        formatError: ({ kind, error, file, restriction }) => {
            if (kind === "restriction") {
                switch (restriction.code) {
                    case "maxFileSize":
                        return t("FileUploader.restrictions.maxFileSize", {
                            fileName: file?.name ?? "",
                            maxFileSize: restriction.maxFileSize,
                        });
                    case "fileType":
                        return t("FileUploader.restrictions.fileType", {
                            fileName: file?.name ?? "",
                            types: restriction.acceptedFileTypes.join(", "),
                        });
                    case "maxNumberOfFiles":
                        return t("FileUploader.restrictions.maxNumberOfFiles", {
                            maximum: restriction.maxNumberOfFiles,
                        });
                    case "duplicate":
                        return t("FileUploader.restrictions.duplicate", { fileName: file?.name ?? "" });
                    case "unknown":
                        return t("FileUploader.restrictions.unknown");
                }
            }
            return kind === "validation"
                ? t("FileUploader.resourceCheckError", { error: error.message })
                : t("FileUploader.uploadError", { errorDetails: error.message, fileName: file?.name ?? "" });
        },
        uploadProgress: t("FileUploader.uploadProgress"),
        uploadedFile: (file) => t("FileUploader.successfullyUploaded", { uploadedName: file.name }),
    };
};
