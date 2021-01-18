import {
    ContextMenu,
    Highlighter,
    IconButton,
    MenuItem,
    OverviewItem,
    OverviewItemActions,
    OverviewItemDescription,
    OverviewItemLine,
} from "@gui-elements/index";
import { getItemLinkIcons } from "../../../utils/getItemLinkIcons";
import Tag from "@gui-elements/src/components/Tag/Tag";
import { ResourceLink } from "../ResourceLink/ResourceLink";
import React from "react";
import { IRelatedItem } from "@ducks/shared/typings";
import { useTranslation } from "react-i18next";
import { routerOp } from "@ducks/router";
import { useDispatch } from "react-redux";
import { useIFrameWindowLinks } from "../IframeWindow/iframewindowHooks";

interface IProps {
    // The related item to be shown
    relatedItem: IRelatedItem;
    // Optional text query to highlight matching text parts of the item.
    textQuery?: string;
}

export function RelatedItem({ relatedItem, textQuery }: IProps) {
    const [t] = useTranslation();
    const dispatch = useDispatch();
    const { iframeWindow, toggleIFrameLink } = useIFrameWindowLinks(relatedItem.itemLinks.slice(1));

    // Go to details page of related item
    const goToDetailsPage = (relatedItem, event) => {
        if (!event?.ctrlKey) {
            event.preventDefault();
            dispatch(
                routerOp.goToPage(relatedItem.itemLinks[0].path, {
                    taskLabel: relatedItem.label,
                    itemType: relatedItem.type.toLowerCase(),
                })
            );
        }
    };

    const contextMenuItems = relatedItem.itemLinks.map((link, idx) => (
        <MenuItem
            key={link.path}
            text={link.label}
            href={link.path}
            icon={getItemLinkIcons(link.label)}
            onClick={
                idx === 0
                    ? (e) => goToDetailsPage(relatedItem, e)
                    : (e) => {
                          e.preventDefault();
                          e.stopPropagation();
                          toggleIFrameLink(link);
                      }
            }
        />
    ));
    return (
        <OverviewItem key={relatedItem.id} densityHigh>
            <OverviewItemDescription>
                <OverviewItemLine>
                    <span>
                        <Tag>
                            <Highlighter label={relatedItem.type} searchValue={textQuery} />
                        </Tag>{" "}
                        <ResourceLink
                            url={!!relatedItem.itemLinks.length ? relatedItem.itemLinks[0].path : false}
                            handlerResourcePageLoader={
                                !!relatedItem.itemLinks.length ? (e) => goToDetailsPage(relatedItem, e) : false
                            }
                        >
                            <Highlighter label={relatedItem.label} searchValue={textQuery} />
                        </ResourceLink>
                    </span>
                </OverviewItemLine>
            </OverviewItemDescription>
            <OverviewItemActions>
                {!!relatedItem.itemLinks.length && (
                    <IconButton
                        name="item-viewdetails"
                        text={t("common.action.showDetails", "Show details")}
                        onClick={(e) => goToDetailsPage(relatedItem, e)}
                        href={relatedItem.itemLinks[0].path}
                    />
                )}
                {contextMenuItems.length && (
                    <ContextMenu togglerText={t("common.action.moreOptions", "Show more options")}>
                        {contextMenuItems}
                    </ContextMenu>
                )}
            </OverviewItemActions>
            {iframeWindow}
        </OverviewItem>
    );
}
