import React, { useEffect, useState } from "react";
import { Button, Notification, SimpleDialog } from "@gui-elements/index";
import useHotKey from "../HotKeyHandler/HotKeyHandler";
import { useTranslation } from "react-i18next";
import { recentlyViewedItems } from "@ducks/workspace/requests";
import { IRecentlyViewedItem } from "@ducks/workspace/typings";
import { ErrorResponse } from "../../../services/fetch/responseInterceptor";
import { Loading } from "../Loading/Loading";
import { extractSearchWords } from "../Highlighter/Highlighter";
import { IItemLink } from "@ducks/shared/typings";
import { useDispatch } from "react-redux";
import { routerOp } from "@ducks/router";
import { Autocomplete } from "../Autocomplete/Autocomplete";

export function RecentlyViewedModal() {
    const [isOpen, setIsOpen] = useState(false);
    const [recentItems, setRecentItems] = useState<IRecentlyViewedItem[]>([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<ErrorResponse | null>(null);
    const { t } = useTranslation();
    const dispatch = useDispatch();
    const loadRecentItems = async () => {
        setError(null);
        try {
            setLoading(true);
            const recentItems = await recentlyViewedItems();
            setRecentItems(recentItems.data);
        } catch (ex) {
            if (ex.isFetchError && ex.errorResponse) {
                setError(ex.errorResponse);
            } else {
                throw ex;
            }
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        if (isOpen) {
            loadRecentItems();
        }
    }, [isOpen]);

    useHotKey({
        hotkey: "ctrl+shift+f",
        handler: () => setIsOpen(true),
    });
    const close = () => setIsOpen(false);
    const onChange = (itemLinks: IItemLink[]) => {
        setIsOpen(false);
        if (itemLinks.length > 0) {
            const itemLink = itemLinks[0];
            dispatch(routerOp.goToPage(itemLink.path));
        }
    };
    const itemLabel = (item: IRecentlyViewedItem) => {
        const projectLabel = item.projectLabel ? item.projectLabel : item.projectId;
        const taskLabel = item.taskLabel ? item.taskLabel : item.taskId;
        return taskLabel ? `${taskLabel} (${projectLabel})` : projectLabel;
    };
    // Searches on the results from the initial requests
    const onSearch = (textQuery: string) => {
        const searchWords = extractSearchWords(textQuery);
        return recentItems.filter((item) => {
            const label = itemLabel(item).toLowerCase();
            return searchWords.every((word) => label.includes(word));
        });
    };
    // Auto-completion parameters necessary for auto-completion widget. FIXME: This shouldn't be needed.
    const autoCompletion = {
        allowOnlyAutoCompletedValues: true,
        autoCompleteValueWithLabels: true,
        autoCompletionDependsOnParameters: [],
    };
    // Warning when an error has occurred
    const errorView = () => {
        return (
            <Notification danger>
                <span>
                    {error.title}. {error.detail ? ` Details: ${error.detail}` : ""}
                </span>
            </Notification>
        );
    };
    // If there are no recent items to display
    const emptyView = () => {
        return (
            <div data-test-id={"recently-viewed-modal-empty"}>
                <Notification>{t("RecentlyViewedModal.emptyList")}</Notification>
            </div>
        );
    };
    // The auto-completion of the recently viewed items
    const recentlyViewedAutoCompletion = () => {
        return (
            <Autocomplete<IRecentlyViewedItem, IItemLink[]>
                onSearch={onSearch}
                autoCompletion={autoCompletion}
                itemValueSelector={(value) => value.itemLinks}
                itemLabelRenderer={itemLabel}
                onChange={onChange}
                autoFocus={true}
            />
        );
    };
    return (
        <SimpleDialog
            transitionDuration={20}
            onClose={close}
            isOpen={isOpen}
            title={t("RecentlyViewedModal.title")}
            actions={<Button onClick={close}>{t("common.action.close")}</Button>}
        >
            {loading ? (
                <Loading />
            ) : error ? (
                errorView()
            ) : recentItems.length === 0 ? (
                emptyView()
            ) : (
                recentlyViewedAutoCompletion()
            )}
        </SimpleDialog>
    );
}
