import React, { useEffect, useState } from "react";
import { useDispatch } from "react-redux";
import ReactDOM from "react-dom";
import { Helmet } from "react-helmet";
import {
    BreadcrumbList,
    Icon,
    OverflowText,
    OverviewItem,
    OverviewItemActions,
    OverviewItemDepiction,
    OverviewItemDescription,
    OverviewItemLine,
    TitlePage,
    BreadcrumbItemProps,
} from "@eccenca/gui-elements";
import { routerOp } from "@ducks/router";
import { APPLICATION_CORPORATION_NAME, APPLICATION_SUITE_NAME } from "../../../constants/base";
import { fetchBreadcrumbs } from "./breadcrumbsHelper";
import ItemDepiction from "../ItemDepiction";
import { convertTaskTypeToItemType, TaskType } from "@ducks/shared/typings";

interface IPageHeaderContentBasicProps extends React.HTMLAttributes<HTMLDivElement> {
    /** Optional type of the page item. Can be an ItemType. */
    type?: string;
    /** Optional plugin ID of the page item. */
    pluginId?: string;
    alternateDepiction?: string;
    breadcrumbs?: BreadcrumbItemProps[];
    breadcrumbsExtensions?: BreadcrumbItemProps[]; // breadcrumbs additional to the autogenerated ones
    autogenerateBreadcrumbs?: boolean;
    pageTitle?: string;
    autogeneratePageTitle?: boolean;
    actionsMenu?: JSX.Element;
}

/*
interface IPageHeaderContentAutogenerateBreadcrumbsProps extends IPageHeaderContentBasicProps {
    autogenerateBreadcrumbs: true;
    breadcrumbs?: never;
}

interface IPageHeaderContentAutogeneratePageTitleProps extends IPageHeaderContentAutogenerateBreadcrumbsProps {
    autogeneratePageTitle: true;
    pageTitle?: never;
}

type TPageHeader = IPageHeaderContentBasicProps &
    IPageHeaderContentAutogenerateBreadcrumbsProps &
    IPageHeaderContentAutogeneratePageTitleProps;
*/

export const APP_VIEWHEADER_ID = "diapp__viewheader";

// Header element that uses portal function to get rendered in one special place regardless from where it is used
export function PageHeader({ autogenerateBreadcrumbs = false, ...headerProps }: IPageHeaderContentBasicProps) {
    const PageHeader = autogenerateBreadcrumbs ? PageHeaderContentWithBreadCrumbs : PageHeaderContent;

    return (
        <PageHeaderPortal>
            <PageHeader {...headerProps} />
        </PageHeaderPortal>
    );
}

function PageHeaderPortal({ children }: any) {
    const [portalEnabled, setPortalEnabled] = useState(false);
    const portalTarget = document.getElementById(APP_VIEWHEADER_ID);

    useEffect(() => {
        if (portalTarget && !portalEnabled) {
            portalTarget.innerHTML = "";
            setPortalEnabled(true);
        }
    });

    return portalEnabled && portalTarget ? ReactDOM.createPortal(<>{children}</>, portalTarget) : <></>;
}

function PageHeaderContent({
    type,
    pluginId,
    alternateDepiction,
    breadcrumbs,
    breadcrumbsExtensions,
    pageTitle,
    autogeneratePageTitle = false,
    actionsMenu,
}: IPageHeaderContentBasicProps) {
    const dispatch = useDispatch();

    const handleBreadcrumbItemClick = React.useCallback((itemUrl, e) => {
        e.preventDefault();
        if (itemUrl) {
            dispatch(routerOp.goToPage(itemUrl, {}));
        }
    }, []);

    const generatedPageTitle =
        autogeneratePageTitle && breadcrumbs && breadcrumbs.length > 0
            ? (breadcrumbs[breadcrumbs.length - 1].text ?? "").toString()
            : "";

    const renderWindowTitle = () => {
        const typeinfo = !!type ? `(${type})` : "";
        const position =
            !!breadcrumbs && breadcrumbs.length > 1
                ? "at " +
                  breadcrumbs
                      .slice(0, breadcrumbs.length - 1)
                      .map((o) => o.text)
                      .join(" / ")
                : "";
        const brandingSuffix =
            APPLICATION_CORPORATION_NAME() || APPLICATION_SUITE_NAME()
                ? ` — ${APPLICATION_CORPORATION_NAME()} ${APPLICATION_SUITE_NAME()}`
                : "";

        return `${pageTitle || generatedPageTitle} ${typeinfo} ${position}${brandingSuffix}`;
    };

    const getDepictionIcons = () => {
        const iconNames: string[] = [];
        if (!!type) {
            // The type is either a TaskType or an ItemType. If already an ItemType, keep ItemType.
            iconNames.push("artefact-" + convertTaskTypeToItemType(type as TaskType, true).toLowerCase());
        }
        if (!!alternateDepiction) {
            iconNames.push(alternateDepiction);
        }
        return iconNames;
    };

    let iconNames = getDepictionIcons();

    return (
        <>
            <Helmet title={renderWindowTitle()} />
            <OverviewItem>
                {iconNames.length > 0 && (
                    <OverviewItemDepiction>
                        {type && pluginId ? (
                            <ItemDepiction itemType={type} pluginId={pluginId} />
                        ) : (
                            <Icon name={iconNames} large />
                        )}
                    </OverviewItemDepiction>
                )}
                <OverviewItemDescription>
                    {(!!breadcrumbs || !!breadcrumbsExtensions) && (
                        <OverviewItemLine small>
                            <BreadcrumbList
                                items={(breadcrumbs ?? []).concat(breadcrumbsExtensions ?? [])}
                                onItemClick={handleBreadcrumbItemClick}
                            />
                        </OverviewItemLine>
                    )}
                    {(!!pageTitle || !!generatedPageTitle) && (
                        <OverviewItemLine large>
                            <TitlePage>
                                <h1>
                                    <OverflowText>{pageTitle || generatedPageTitle}</OverflowText>
                                </h1>
                            </TitlePage>
                        </OverviewItemLine>
                    )}
                </OverviewItemDescription>
                {!!actionsMenu && <OverviewItemActions>{actionsMenu}</OverviewItemActions>}
            </OverviewItem>
        </>
    );
}

const PageHeaderContentWithBreadCrumbs = fetchBreadcrumbs(PageHeaderContent);

// Custom hook to update single properties of the element
export function usePageHeader({ ...propsHeader }: IPageHeaderContentBasicProps) {
    const [pageHeaderProps, setPageHeaderProps] = useState<IPageHeaderContentBasicProps>({ ...propsHeader });

    const updatePageHeader = (propsUpdate: IPageHeaderContentBasicProps) => {
        setPageHeaderProps({ ...pageHeaderProps, ...propsUpdate });
    };

    const pageHeader = <PageHeader {...pageHeaderProps} />;
    return {
        pageHeader,
        updateType: (itemType: string, pluginId?: string) => {
            updatePageHeader({ type: itemType, pluginId });
        },
        updatePageTitle: (update) => {
            updatePageHeader({ pageTitle: update, autogeneratePageTitle: false });
        },
        updateBreadcrumbs: (update) => {
            updatePageHeader({ breadcrumbs: update, autogenerateBreadcrumbs: false });
        },
        updateBreadcrumbsExtensions: (update) => {
            updatePageHeader({ breadcrumbsExtensions: update, autogenerateBreadcrumbs: true });
        },
        updateActionsMenu: (update: JSX.Element) => {
            updatePageHeader({ actionsMenu: update });
        },
        updatePageHeader,
    } as const;
}
