import jsdom from 'jsdom';
import _ from 'lodash';

const { JSDOM } = jsdom;

const dom = new JSDOM('<!doctype html><html><body></body></html>');
const win = dom.window;
const doc = win.document;

global.document = doc;
global.window = global.document.defaultView;

// This is referenced amongst others in rxjs
global.window.Object = Object;
global.window.Math = Math;

Object.keys(global.window).forEach((key) => {
    if (!(key in global)) {
        global[key] = window[key];
    }
});

// This is expected by some ecc gui elements components and totally custom, this should be fixed in the gui elements itself in the future
global.__WEBPACK__ = false;
global.__DEBUG__ = false;

// Code mirror needs this
global.document.body.createTextRange = function() {
    return {
        setEnd: function(){},
        setStart: function(){},
        getBoundingClientRect: function(){
            return {right: 0};
        },
        getClientRects: function(){
            return {
                length: 0,
                left: 0,
                right: 0
            }
        }
    }
};

/**
 * mock of sessionStorage
 *
 * @constructor
 */
function SessionStorage() {
    /**
     * @type {*}
     */

    this.data = {};
    /**
     * @param key {string}
     * @param value {*}
     */

    this.setItem = (key, value) => {
        this.data[key] = value;
    };
    /**
     * @param key
     * @returns {*}
     */

    this.getItem = key => this.data[key];

    /**
     * @param key {string}
     * @returns {boolean}
     */
    this.removeItem = key => delete this.data[key];
}

// Needed for MDL
global.HTMLElement = win.HTMLElement;
global.Element = global.window.Element;
global.CustomEvent = global.window.CustomEvent;
global.NodeList = global.window.NodeList;
global.Node = global.window.Node;
global._ = _

export default (component) => {
    const waitUntilComponentFinishes = async (component) => {
        await wait();
        await flushPromises();
        component.update();
    };

    const flushPromises = () => {
        return new Promise(resolve => setImmediate(resolve));
    };

    function wait() {
        return new Promise(resolve => {
            setTimeout(resolve, timeout)
        });
    }

    const timeout = 20;

    return waitUntilComponentFinishes(component)
}

export {SessionStorage};
