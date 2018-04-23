import {findDOMNode} from 'react-dom';
import _ from 'lodash';

const MaterialMixin = {
    // materialDesign,
    componentDidMount() {
        if (__DEBUG__) {
            console.debug(
                `MaterialMixin is used in ${this.constructor.displayName}. ` +
                    `Please check if this usage is wanted and migrate old MDL Stuff to new gui-elements.` +
                    `MaterialMixin may get deprecated soon.`
            );
        }
        window.componentHandler.upgradeDom();
    },
    componentWillUnmount() {
        const comp = findDOMNode(this);
        if (!_.isNull(comp)) {
            window.componentHandler.downgradeElements(comp);
        }
    },
    componentDidUpdate() {
        const comp = findDOMNode(this);
        if (!_.isNull(comp)) {
            window.componentHandler.upgradeElements(comp);
        }
    },
};

export default MaterialMixin;
