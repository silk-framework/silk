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
} from "@gui-elements/index";
import Loading from "../../../shared/Loading";
import FileUploadModal from "../../../shared/modals/FileUploadModal";
import { EmptyFileWidget } from "./EmptyFileWidget";
import { SearchBar } from "../../../shared/SearchBar/SearchBar";
import { Highlighter } from "../../../shared/Highlighter/Highlighter";
import { usePagination } from "@gui-elements/src/components/Pagination/Pagination";
import { commonSel } from "@ducks/common";
import { useTranslation } from "react-i18next";
import { FileRemoveModal } from "../../../shared/modals/FileRemoveModal";

/** Project file management widget. */
export const FileWidget = () => {
    const dispatch = useDispatch();

    const filesList = useSelector(workspaceSel.filesListSelector);
    const fileWidget = useSelector(workspaceSel.widgetsSelector).files;
    const [textQuery, setTextQuery] = useState("");
    const projectId = useSelector(commonSel.currentProjectIdSelector);

    const [isOpenDialog, setIsOpenDialog] = useState<boolean>(false);

    // contains file item
    const [fileDeleteDialog, setFileDeleteDialog] = useState(null);

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
        if (!isOpenDialog && !fileDeleteDialog) {
            const filter: any = {
                limit: 1000,
            };
            if (textQuery) {
                filter.searchText = textQuery;
            }
            dispatch(workspaceOp.fetchResourcesListAsync(filter));
        }
    }, [textQuery, isOpenDialog, fileDeleteDialog]);

    const toggleFileUploader = () => {
        setIsOpenDialog(!isOpenDialog);
    };

    return (
        <>
            <Card>
                <CardHeader>
                    <CardTitle>
                        <h2>{t("widget.FileWidget.files", "Files")}</h2>
                    </CardTitle>
                </CardHeader>
                <Divider />
                <CardContent>
                    {isLoading ? (
                        <Loading description={t("widget.FileWidget.loading", "Loading file list.")} />
                    ) : !!filesList.length ? (
                        <>
                            <Toolbar>
                                <ToolbarSection canGrow>
                                    <SearchBar textQuery={textQuery} onSearch={onSearch} />
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
                            <Spacing size="tiny" />
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
                                                    <TableCell key={"fileActions"} className="bx--table-column-menu">
                                                        <IconButton
                                                            name="item-remove"
                                                            text={t("common.action.DeleteSmth", {
                                                                smth: t("widget.FileWidget.file"),
                                                            })}
                                                            small
                                                            disruptive
                                                            onClick={() => setFileDeleteDialog(file)}
                                                        />
                                                    </TableCell>
                                                </TableRow>
                                            ))}
                                    </TableBody>
                                </Table>
                            </TableContainer>
                            {filesList.length > Math.min(pagination.total, pagination.minPageSize) ? ( // Don't show if no pagination is needed
                                <>{paginationElement}</>
                            ) : null}
                        </>
                    ) : (
                        <EmptyFileWidget onFileAdd={toggleFileUploader} />
                    )}
                </CardContent>
            </Card>
            <FileUploadModal
                isOpen={isOpenDialog}
                onDiscard={toggleFileUploader}
                uploaderOptions={{ allowMultiple: true }}
            />
            <FileRemoveModal
                projectId={projectId}
                onConfirm={() => setFileDeleteDialog(null)}
                file={fileDeleteDialog}
            />
        </>
    );
};
