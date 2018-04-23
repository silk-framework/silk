import _ from 'lodash';

const getDisplayName = function(Component) {
    const objectConstructor = Object.getPrototypeOf(Component).constructor;
    const objectTest =
        typeof objectConstructor !== 'undefined'
            ? objectConstructor
            : Component;
    return (
        objectTest.displayName ||
        objectTest.name ||
        (typeof objectTest === 'string' ? objectTest : objectTest.toString())
    );
};

const showDifferences = function(object, config, nextConfig) {
    if (__DEBUG__) {
        const componentName = getDisplayName(object);
        const diff = _.reduce(
            object[config],
            (result, value, key) =>
                _.isEqual(value, nextConfig[key]) ? result : result.concat(key),
            []
        );
        console.log(
            `${componentName}.${config} not equal for: ${diff.join(', ')}`
        );
    }
};

const PerformanceMixin = {
    shouldComponentUpdate(nextProps, nextState) {
        if (_.isEqual(nextState, this.state) === false) {
            if (__DEBUG__ && window.enablePerformanceMixingLog) {
                showDifferences(this, 'state', nextState);
            }
            return true;
        }

        if (_.isEqual(nextProps, this.props) === false) {
            if (__DEBUG__ && window.enablePerformanceMixingLog) {
                showDifferences(this, 'props', nextProps);
            }
            return true;
        }

        return false;
    },
};

export default PerformanceMixin;
