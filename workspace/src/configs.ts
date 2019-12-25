import { ComponentClass } from "react";

type SimpleReactComponent = ComponentClass<any, any>;

export interface IEntryPoint {
    component: SimpleReactComponent;
    route: string;
}

export interface IPlugins {
    name: string;
    menuName: string;
    entryPoint: IEntryPoint;
    internal: boolean;
}

interface IDev {
    monitorPerformance: boolean;
    enableStoreDevUtils: boolean;
}

export interface ISettings {
    plugins: IPlugins[];
    dev: IDev;
}

const configs: ISettings = {
    plugins: [
        // {
        //     name: 'InternalDummyComponent',
        //     entryPoint: require('./internalPlugin').default,
        //     internal: true,
        //     menuName: 'Internal'
        // }
    ],
    dev: {
        monitorPerformance: true,
        enableStoreDevUtils: false
    }
};

export default configs;
