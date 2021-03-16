import React, { useState, useEffect } from "react";
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
} from "@gui-elements/index";
import { IBreadcrumbItemProps } from "@gui-elements/src/components/Breadcrumb/BreadcrumbItem";
import { routerOp } from "@ducks/router";
import { APPLICATION_CORPORATION_NAME, APPLICATION_SUITE_NAME } from "../../../constants/base";
import { ActionsMenu } from "../ActionsMenu/ActionsMenu";
import { fetchBreadcrumbs } from "./breadcrumbsHelper";

interface IPageHeaderContentBasicProps extends React.HTMLAttributes<HTMLDivElement> {
    type?: string;
    alternateDepiction?: string;
    breadcrumbs?: IBreadcrumbItemProps[];
    autogenerateBreadcrumbs?: boolean;
    pageTitle?: string;
    autogeneratePageTitle?: boolean;
    actionsMenu?: typeof ActionsMenu;
}

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

    return portalEnabled ? ReactDOM.createPortal(<>{children}</>, portalTarget) : <></>;
}

function PageHeaderContent({
    type,
    alternateDepiction,
    breadcrumbs,
    pageTitle,
    autogeneratePageTitle = false,
    actionsMenu,
}: IPageHeaderContentBasicProps) {
    const dispatch = useDispatch();

    const handleBreadcrumbItemClick = (itemUrl, e) => {
        e.preventDefault();
        if (itemUrl) {
            dispatch(routerOp.goToPage(itemUrl, {}));
        }
    };

    const generatedPageTitle =
        autogeneratePageTitle && breadcrumbs && breadcrumbs.length > 0
            ? breadcrumbs[breadcrumbs.length - 1].text.toString()
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
        const application = `${APPLICATION_CORPORATION_NAME} ${APPLICATION_SUITE_NAME}`;

        return `${pageTitle || generatedPageTitle} ${typeinfo} ${position} — ${application}`;
    };

    const getDepictionIcons = () => {
        const iconNames = [];
        if (!!type) {
            iconNames.push("artefact-" + type);
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
                        <Icon name={iconNames} large />
                    </OverviewItemDepiction>
                )}
                <OverviewItemDescription>
                    {!!breadcrumbs && (
                        <OverviewItemLine small>
                            <BreadcrumbList items={breadcrumbs} onItemClick={handleBreadcrumbItemClick} />
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
        updateType: (update) => {
            updatePageHeader({ type: update });
        },
        updatePageTitle: (update) => {
            updatePageHeader({ pageTitle: update, autogeneratePageTitle: false });
        },
        updateBreadcrumbs: (update) => {
            updatePageHeader({ breadcrumbs: update, autogenerateBreadcrumbs: false });
        },
        updateActionsMenu: (update) => {
            updatePageHeader({ actionsMenu: update });
        },
        updatePageHeader,
    } as const;
}
