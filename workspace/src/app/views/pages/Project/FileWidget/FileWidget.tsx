import React, { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { workspaceOp, workspaceSel } from "@ducks/workspace";
import {
    Card,
    CardContent,
    CardHeader,
    CardOptions,
    CardTitle,
    Divider,
    ClassNames,
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
import { useTranslation, TFunction } from "react-i18next";
import { FileRemoveModal } from "../../../shared/modals/FileRemoveModal";
import { CONTEXT_PATH } from "../../../../constants/path";
import { fileValue, IProjectResource } from "@ducks/shared/typings";
import { AppDispatch } from "store/configureStore";
import { GlobalTableContext } from "../../../../GlobalContextsWrapper";
import { SortModifierType } from "@ducks/workspace/typings";

type FileSortColumn = "name" | "modified" | "size";

interface IFileTableHeader {
    key: FileSortColumn;
    header: string;
    highlighted: boolean;
}

interface IProjectFileRow extends IProjectResource {
    id: string;
    formattedDate: string;
    formattedSize: string;
}

interface IFileTableProps {
    filesList: IProjectFileRow[];
    headers: IFileTableHeader[];
    projectId?: string;
    textQuery: string;
    onDelete(file: IProjectFileRow): void;
}

const compareOptionalNumbers = (
    left: number | undefined,
    right: number | undefined,
    sortOrder: SortModifierType | undefined,
): number => {
    const leftMissing = left === undefined || Number.isNaN(left);
    const rightMissing = right === undefined || Number.isNaN(right);

    if (leftMissing && rightMissing) {
        return 0;
    } else if (leftMissing) {
        return 1;
    } else if (rightMissing) {
        return -1;
    }

    const result = left - right;
    return sortOrder === "DESC" ? result * -1 : result;
};

const nextSortState = (
    currentSortBy: string | undefined,
    currentSortOrder: SortModifierType | undefined,
    sortBy: FileSortColumn,
): { sortBy: string; sortOrder: SortModifierType } => {
    if (currentSortBy !== sortBy) {
        return { sortBy, sortOrder: "ASC" };
    } else if (currentSortOrder === "ASC") {
        return { sortBy, sortOrder: "DESC" };
    } else {
        return { sortBy: "", sortOrder: "" };
    }
};

const sortIconName = (
    currentSortBy: string | undefined,
    currentSortOrder: SortModifierType | undefined,
    sortBy: FileSortColumn,
) => {
    if (currentSortBy !== sortBy) {
        return "list-sort";
    } else if (currentSortOrder === "ASC") {
        return "list-sortasc";
    } else {
        return "list-sortdesc";
    }
};

const sortButtonText = (
    t: TFunction,
    currentSortBy: string | undefined,
    currentSortOrder: SortModifierType | undefined,
    sortBy: FileSortColumn,
) => {
    if (currentSortBy !== sortBy) {
        return t("common.action.sortColumn.ascending", "Sort column: ascending");
    } else if (currentSortOrder === "ASC") {
        return t("common.action.sortColumn.descending", "Sort column: descending");
    } else {
        return t("common.action.sortColumn.remove", "Sort column: remove");
    }
};

function FileTable({ filesList, headers, projectId, textQuery, onDelete }: IFileTableProps) {
    const { updateGlobalTableSettings, globalTableSettings } = React.useContext(GlobalTableContext);
    const [t] = useTranslation();
    const [pagination, paginationElement, onTotalChange] = usePagination({
        pageSizes: [5, 10, 20],
        presentation: { hideInfoText: true },
        initialPageSize: globalTableSettings["files"].pageSize,
    });

    React.useEffect(() => {
        updateGlobalTableSettings({ pageSize: pagination.limit }, "files");
    }, [pagination.limit, updateGlobalTableSettings]);

    const sortBy = globalTableSettings["files"].sortBy ?? "";
    const sortOrder = globalTableSettings["files"].sortOrder ?? "";
    const collator = new Intl.Collator(undefined, { numeric: true, sensitivity: "base" });
    const sortedFiles = [...filesList].sort((left, right) => {
        let result = 0;
        switch (sortBy) {
            case "name":
                result = collator.compare(fileValue(left), fileValue(right));
                break;
            case "modified":
                result = compareOptionalNumbers(
                    left.modified ? Date.parse(left.modified) : undefined,
                    right.modified ? Date.parse(right.modified) : undefined,
                    sortOrder,
                );
                break;
            case "size":
                result = compareOptionalNumbers(left.size, right.size, sortOrder);
                break;
            default:
                return 0;
        }

        if (result === 0) {
            result = collator.compare(fileValue(left), fileValue(right));
        }
        return sortBy === "name" && sortOrder === "DESC" ? result * -1 : result;
    });

    if (sortedFiles.length !== pagination.total) {
        onTotalChange(sortedFiles.length);
    }

    return (
        <>
            <TableContainer>
                <Table size="small" columnWidths={["40%", "25%", "25%", "60px"]}>
                    <TableHead>
                        <TableRow>
                            {headers.map((property) => (
                                <TableHeader key={property.key}>
                                    <Toolbar noWrap={true}>
                                        <ToolbarSection canShrink={true}>{property.header}</ToolbarSection>
                                        <ToolbarSection>
                                            <Spacing size="tiny" vertical={true} />
                                            <IconButton
                                                size={"small"}
                                                data-test-id={`project-files-sort-${property.key}`}
                                                name={sortIconName(sortBy, sortOrder, property.key)}
                                                text={sortButtonText(t, sortBy, sortOrder, property.key)}
                                                onClick={() =>
                                                    updateGlobalTableSettings(
                                                        nextSortState(sortBy, sortOrder, property.key),
                                                        "files",
                                                    )
                                                }
                                            />
                                        </ToolbarSection>
                                    </Toolbar>
                                </TableHeader>
                            ))}
                            <TableHeader key={"fileActions"}>{""}</TableHeader>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {sortedFiles
                            .slice((pagination.current - 1) * pagination.limit, pagination.current * pagination.limit)
                            .map((file) => (
                                <TableRow key={file.id}>
                                    {headers.map((property) => {
                                        const value =
                                            property.key === "name"
                                                ? fileValue(file)
                                                : property.key === "modified"
                                                  ? file.formattedDate
                                                  : file.formattedSize;
                                        return (
                                            <TableCell
                                                alignVertical="middle"
                                                key={property.key}
                                                className={
                                                    property.key === "name" ? ClassNames.Typography.FORCELINEBREAK : ""
                                                }
                                            >
                                                {property.highlighted ? (
                                                    <Highlighter label={value} searchValue={textQuery} />
                                                ) : (
                                                    value
                                                )}
                                            </TableCell>
                                        );
                                    })}
                                    <TableCell alignVertical="middle" key={"fileActions"}>
                                        <div style={{ display: "flex" }}>
                                            <IconButton
                                                data-test-id={"resource-download-btn"}
                                                name="item-download"
                                                text={t("common.action.download")}
                                                small
                                                href={`${CONTEXT_PATH}/workspace/projects/${projectId}/files?path=${encodeURIComponent(
                                                    fileValue(file),
                                                )}`}
                                            />
                                            <IconButton
                                                name="item-remove"
                                                data-test-id={"resource-delete-btn"}
                                                text={t("common.action.DeleteSmth", {
                                                    smth: t("widget.FileWidget.file"),
                                                })}
                                                small
                                                disruptive
                                                onClick={() => onDelete(file)}
                                            />
                                        </div>
                                    </TableCell>
                                </TableRow>
                            ))}
                    </TableBody>
                </Table>
            </TableContainer>
            {sortedFiles.length > Math.min(pagination.total, pagination.minPageSize) && paginationElement}
        </>
    );
}

/** Project file management widget. */
export const FileWidget = () => {
    const dispatch = useDispatch<AppDispatch>();
    const { updateGlobalTableSettings } = React.useContext(GlobalTableContext);

    const filesList = useSelector(workspaceSel.filesListSelector) as IProjectFileRow[];
    const fileWidget = useSelector(workspaceSel.widgetsSelector).files;
    const [textQuery, setTextQuery] = useState("");
    const [paginationResetKey, setPaginationResetKey] = useState(0);
    const projectId = useSelector(commonSel.currentProjectIdSelector);

    const [isOpenDialog, setIsOpenDialog] = useState<boolean>(false);

    // contains file item
    const [fileDeleteDialog, setFileDeleteDialog] = useState<IProjectFileRow | null>(null);

    const { isLoading } = fileWidget;
    const [t] = useTranslation();

    useEffect(() => {
        updateGlobalTableSettings({ sortBy: "", sortOrder: "" }, "files");
        return () => {
            updateGlobalTableSettings({ sortBy: "", sortOrder: "" }, "files");
        };
    }, [updateGlobalTableSettings]);

    const headers: IFileTableHeader[] = [
        { key: "name", header: t("widget.FileWidget.sort.name", "Name"), highlighted: true },
        { key: "modified", header: t("widget.FileWidget.sort.modified", "Last modified"), highlighted: false },
        { key: "size", header: t("widget.FileWidget.sort.size", "Size (bytes)"), highlighted: true },
    ];

    const onSearch = (newTextQuery: string) => {
        if (newTextQuery !== textQuery) {
            setPaginationResetKey((currentKey) => currentKey + 1);
        }
        setTextQuery(newTextQuery);
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
    }, [dispatch, textQuery, isOpenDialog, fileDeleteDialog, projectId]);

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
                    <CardOptions>
                        <IconButton
                            name="item-upload"
                            data-test-id="project-files-widget-add-file-btn"
                            text={t("FileUploader.modalTitle", "Upload file")}
                            onClick={toggleFileUploader}
                        />
                    </CardOptions>
                </CardHeader>
                <Divider />
                <CardContent style={{ maxHeight: "25vh" }}>
                    {isLoading ? (
                        <Loading description={t("widget.FileWidget.loading", "Loading file list.")} />
                    ) : (
                        <>
                            {(!!textQuery || !!filesList.length) && (
                                <SearchBar
                                    textQuery={textQuery}
                                    onSearch={onSearch}
                                    data-test-id={"file-search-bar"}
                                    focusOnCreation={!!textQuery.length}
                                    globalTableKey={"files"}
                                />
                            )}
                            {!!filesList.length && <Spacing size="tiny" />}
                            {!!filesList.length && (
                                <FileTable
                                    key={paginationResetKey}
                                    filesList={filesList}
                                    headers={headers}
                                    projectId={projectId}
                                    textQuery={textQuery}
                                    onDelete={(file) => setFileDeleteDialog(file)}
                                />
                            )}
                            {!textQuery && !filesList.length && <EmptyFileWidget />}
                        </>
                    )}
                </CardContent>
            </Card>
            <FileUploadModal
                isOpen={isOpenDialog}
                onDiscard={toggleFileUploader}
                uploaderOptions={{ allowMultiple: true }}
            />
            {projectId && fileDeleteDialog && (
                <FileRemoveModal
                    projectId={projectId}
                    onConfirm={() => setFileDeleteDialog(null)}
                    file={fileDeleteDialog}
                />
            )}
        </>
    );
};
