import { AxiosResponse } from "axios";

export const downloadResource = (axiosResponse: AxiosResponse) => {
    let filename = "";
    const disposition = axiosResponse.headers["content-disposition"];

    if (disposition && disposition.includes("attachment")) {
        const filenameRegex = /filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/;
        const matches = filenameRegex.exec(disposition);

        if (matches != null && matches[1]) {
            filename = matches[1].replace(/['"]/g, "");
        }
    }

    const contentType = axiosResponse.headers["Content-Type"];
    let blob;

    if (typeof File === "function") {
        try {
            blob = new File([axiosResponse.data], filename, { type: contentType });
        } catch {
            /* Edge */
        }
    }

    if (typeof blob === "undefined") {
        blob = new Blob([axiosResponse.data], { type: contentType });
    }
    if (typeof window.navigator.msSaveBlob !== "undefined") {
        // IE workaround for "HTML7007: One or more blob URLs were revoked by closing the blob for which they were created. These URLs will no longer resolve as the data backing the URL has been freed."
        window.navigator.msSaveBlob(blob, filename);
    } else {
        const URL = window.URL || window.webkitURL;
        const downloadUrl = URL.createObjectURL(blob);
        if (filename) {
            // use HTML5 a[download] attribute to specify filename
            const a = document.createElement("a");
            // safari doesn't support this yet
            a.href = downloadUrl;
            a.download = filename;
            document.body.appendChild(a);
            a.click();
        }
        setTimeout(function () {
            URL.revokeObjectURL(downloadUrl);
        }, 100); // cleanup
    }
};
