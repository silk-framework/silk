import React from 'react';
import { H1 as B_H1, H2 as B_H2, H3 as B_H3, H4 as B_H4, H5 as B_H5 } from "@blueprintjs/core";

export const H1 = ({children, ...restProps}: any) => <B_H1 {...restProps}>{children}</B_H1>;
export const H2 = ({children, ...restProps}: any) => <B_H2 {...restProps}>{children}</B_H2>;
export const H3 = ({children, ...restProps}: any) => <B_H3 {...restProps}>{children}</B_H3>;
export const H4 = ({children, ...restProps}: any) => <B_H4 {...restProps}>{children}</B_H4>;
export const H5 = ({children, ...restProps}: any) => <B_H5 {...restProps}>{children}</B_H5>;
