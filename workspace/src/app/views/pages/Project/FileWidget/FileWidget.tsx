import React, { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { workspaceOp, workspaceSel } from "@ducks/workspace";
import {
    Button,
    Card,
    CardContent,
    CardHeader,
    CardTitle,
    Divider,
    Highlighter,
    IconButton,
    Spacing,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableHeader,
    TableRow,
    Toolbar,
    ToolbarSection,
} from "@eccenca/gui-elements";
import Loading from "../../../shared/Loading";
import FileUploadModal from "../../../shared/modals/FileUploadModal";
import { EmptyFileWidget } from "./EmptyFileWidget";
import { SearchBar } from "../../../shared/SearchBar/SearchBar";
import { usePagination } from "@eccenca/gui-elements/src/components/Pagination/Pagination";
import { commonSel } from "@ducks/common";
import { useTranslation } from "react-i18next";
import { FileRemoveModal } from "../../../shared/modals/FileRemoveModal";
import { CONTEXT_PATH } from "../../../../constants/path";

/** Project file management widget. */
export const FileWidget = () => {
    const dispatch = useDispatch();

    const filesList = useSelector(workspaceSel.filesListSelector);
    const fileWidget = useSelector(workspaceSel.widgetsSelector).files;
    const [textQuery, setTextQuery] = useState("");
    const projectId = useSelector(commonSel.currentProjectIdSelector);

    const [isOpenDialog, setIsOpenDialog] = useState<boolean>(false);

    // contains file item
    const [fileDeleteDialog, setFileDeleteDialog] = useState<any>(null);

    const { isLoading } = fileWidget;
    const [pagination, paginationElement, onTotalChange] = usePagination({
        pageSizes: [5, 10, 20],
        presentation: { hideInfoText: true },
    });
    const [t] = useTranslation();

    // @FIXME: Improve logic, fileList can't be null or undefined, check state object
    if (filesList !== undefined && filesList !== null && filesList.length !== pagination.total) {
        onTotalChange(filesList.length);
    }

    const headers = [
        { key: "name", header: t("widget.FileWidget.sort.name", "Name"), highlighted: true },
        { key: "formattedDate", header: t("widget.FileWidget.sort.modified", "Last modified"), highlighted: false },
        { key: "formattedSize", header: t("widget.FileWidget.sort.size", "Size (bytes)"), highlighted: true },
    ];

    const onSearch = (textQuery) => {
        setTextQuery(textQuery);
    };

    useEffect(() => {
        // Only trigger if file upload dialog is closed, since a file may have been uploaded.
        if (!isOpenDialog && !fileDeleteDialog && projectId) {
            const filter: any = {
                limit: 1000,
            };
            if (textQuery) {
                filter.searchText = textQuery;
            }
            dispatch(workspaceOp.fetchResourcesListAsync(filter, projectId));
        }
    }, [textQuery, isOpenDialog, fileDeleteDialog, projectId]);

    const toggleFileUploader = () => {
        setIsOpenDialog(!isOpenDialog);
    };

    return (
        <>
            <Card data-test-id="project-files-widget">
                <CardHeader>
                    <CardTitle>
                        <h2>{t("widget.FileWidget.files", "Files")}</h2>
                    </CardTitle>
                </CardHeader>
                <Divider />
                <CardContent>
                    {isLoading ? (
                        <Loading description={t("widget.FileWidget.loading", "Loading file list.")} />
                    ) : (
                        <>
                            {(!!textQuery || !!filesList.length) && (
                                <Toolbar>
                                    <ToolbarSection canGrow>
                                        <SearchBar
                                            textQuery={textQuery}
                                            onSearch={onSearch}
                                            data-test-id={"file-search-bar"}
                                        />
                                    </ToolbarSection>
                                    <ToolbarSection>
                                        <Spacing size="tiny" vertical />
                                        <Button
                                            elevated
                                            text={t("common.action.AddSmth", { smth: t("widget.FileWidget.file") })}
                                            onClick={toggleFileUploader}
                                        />
                                    </ToolbarSection>
                                </Toolbar>
                            )}
                            {!!filesList.length && <Spacing size="tiny" />}
                            {!!filesList.length && (
                                <TableContainer>
                                    <Table>
                                        <TableHead>
                                            <TableRow>
                                                {headers.map((property) => (
                                                    <TableHeader key={property.key}>{property.header}</TableHeader>
                                                ))}
                                                <TableHeader key={"fileActions"}>{""}</TableHeader>
                                            </TableRow>
                                        </TableHead>
                                        <TableBody>
                                            {filesList
                                                .slice(
                                                    (pagination.current - 1) * pagination.limit,
                                                    pagination.current * pagination.limit
                                                )
                                                .map((file) => (
                                                    <TableRow key={file.id}>
                                                        {headers.map((property) => (
                                                            <TableCell key={property.key}>
                                                                {property.highlighted ? (
                                                                    <Highlighter
                                                                        label={file[property.key]}
                                                                        searchValue={textQuery}
                                                                    />
                                                                ) : (
                                                                    file[property.key]
                                                                )}
                                                            </TableCell>
                                                        ))}
                                                        <TableCell
                                                            key={"fileActions"}
                                                            className="bx--table-column-menu"
                                                        >
                                                            <div style={{ display: "flex" }}>
                                                                <IconButton
                                                                    data-test-id={"resource-download-btn"}
                                                                    name="item-download"
                                                                    text={t("common.action.download")}
                                                                    small
                                                                    href={`${CONTEXT_PATH}/workspace/projects/${projectId}/resources/${encodeURIComponent(file.name)}`}
                                                                />
                                                                <IconButton
                                                                    name="item-remove"
                                                                    data-test-id={"resource-delete-btn"}
                                                                    text={t("common.action.DeleteSmth", {
                                                                        smth: t("widget.FileWidget.file"),
                                                                    })}
                                                                    small
                                                                    disruptive
                                                                    onClick={() => setFileDeleteDialog(file)}
                                                                />
                                                            </div>
                                                        </TableCell>
                                                    </TableRow>
                                                ))}
                                        </TableBody>
                                    </Table>
                                </TableContainer>
                            )}
                            {
                                // Don't show if no pagination is needed
                                filesList.length > Math.min(pagination.total, pagination.minPageSize) &&
                                    paginationElement
                            }
                            {!textQuery && !filesList.length && <EmptyFileWidget onFileAdd={toggleFileUploader} />}
                        </>
                    )}
                </CardContent>
            </Card>
            <FileUploadModal
                isOpen={isOpenDialog}
                onDiscard={toggleFileUploader}
                uploaderOptions={{ allowMultiple: true }}
            />
            {projectId && (
                <FileRemoveModal
                    projectId={projectId}
                    onConfirm={() => setFileDeleteDialog(null)}
                    file={fileDeleteDialog}
                />
            )}
        </>
    );
};
