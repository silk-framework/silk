import React from "react";
// import PropTypes from 'prop-types';
import { Breadcrumbs as BlueprintBreadcrumbList } from "@blueprintjs/core";
import BreadcrumbItem from "./BreadcrumbItem";
import { routerOp } from "@ducks/router";
import { useDispatch } from "react-redux";

function BreadcrumbList({ className = "", itemDivider = "/", ...otherProps }: any) {
    const dispatch = useDispatch();

    const gotoPage = (page) => (e) => {
        e.preventDefault();
        if (page) {
            dispatch(routerOp.goToPage(page, {}));
        }
    };

    const renderBreadcrumb = (propsBreadcrumb) => {
        return <BreadcrumbItem {...propsBreadcrumb} itemDivider="/" onClick={gotoPage(propsBreadcrumb.href)} />;
    };

    const renderCurrentBreadcrumb = (propsBreadcrumb) => {
        return <BreadcrumbItem {...propsBreadcrumb} current={true} href={null} itemDivider={itemDivider} />;
    };

    return (
        <BlueprintBreadcrumbList
            {...otherProps}
            className={"ecc-breadcrumb__list " + className}
            breadcrumbRenderer={renderBreadcrumb}
            currentBreadcrumbRenderer={renderCurrentBreadcrumb}
        />
    );
}

export default BreadcrumbList;
