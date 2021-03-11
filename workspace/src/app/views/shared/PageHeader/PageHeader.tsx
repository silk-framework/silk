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
import { IActionButtonItemProps, TActionsMenuItem, ActionsMenu } from "../ActionsMenu/ActionsMenu";
import { fetchBreadcrumbs } from "./fetchBreadcrumbs";

interface IPageHeaderContentProps extends React.HTMLAttributes<HTMLDivElement> {
    type?: string;
    alternateDepiction?: string;
    breadcrumbs?: IBreadcrumbItemProps[];
    autogenerateBreadcrumbs?: boolean;
    pageTitle?: string;
    autogeneratePageTitle?: boolean;
    actionPrimary?: IActionButtonItemProps;
    actionsSecondary?: IActionButtonItemProps[];
    actionsFullMenu?: TActionsMenuItem[];
}

interface IPageHeaderProps extends IPageHeaderContentProps {
    autogenerateBreadcrumbs?: boolean;
}

export const APP_VIEWHEADER_ID = "diapp__viewheader";

// Header element that uses portal function to get rendered in one special place regardless from where it is used
export function PageHeader({ autogenerateBreadcrumbs = false, ...headerProps }: IPageHeaderProps) {
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
    actionPrimary,
    actionsSecondary,
    actionsFullMenu,
}: IPageHeaderContentProps) {
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
                {(actionPrimary || actionsSecondary || actionsFullMenu) && (
                    <OverviewItemActions>
                        <ActionsMenu
                            actionPrimary={actionPrimary}
                            actionsSecondary={actionsSecondary}
                            actionsFullMenu={actionsFullMenu}
                        />
                    </OverviewItemActions>
                )}
            </OverviewItem>
        </>
    );
}

const PageHeaderContentWithBreadCrumbs = fetchBreadcrumbs(PageHeaderContent);

// Custom hook to update single properties of the element
export const usePageHeader = ({ autogenerateBreadcrumbs = false, ...propsHeader }: IPageHeaderProps) => {
    const [type, setType] = useState<string>(propsHeader.type);
    const [pageTitle, setPageTitle] = useState<string>(propsHeader.pageTitle);
    const [breadcrumbs, setBreadcrumbs] = useState(propsHeader.breadcrumbs);
    /*
    const [actionPrimary, setActionPrimary ] = useState<IActionButtonItemProps>(propsHeader.actionPrimary);
    const [actionsSecondary, setActionsSecondary ] = useState<IActionButtonItemProps[]>(propsHeader.actionsSecondary);
    const [actionsFullMenu, setActionsFullMenu ] = useState<ActionsMenuItem[]>(propsHeader.actionsFullMenu);
    */

    const propsBreadcrumbs = autogenerateBreadcrumbs ? { autogenerateBreadcrumbs: true } : { breadcrumbs: breadcrumbs };

    const pageHeader = (
        <PageHeader
            type={type}
            alternateDepiction={propsHeader.alternateDepiction}
            pageTitle={pageTitle}
            autogeneratePageTitle={propsHeader.autogeneratePageTitle}
            actionPrimary={propsHeader.actionPrimary}
            actionsSecondary={propsHeader.actionsSecondary}
            actionsFullMenu={propsHeader.actionsFullMenu}
            {...propsBreadcrumbs}
        />
    );
    return {
        pageHeader,
        updateType: setType,
        updatePageTitle: setPageTitle,
        updateBreadcrumbs: setBreadcrumbs,
    } as const;
};
