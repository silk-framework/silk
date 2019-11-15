export const getDefaultConfigs = () => {
    return {
        showSizeChanger: true,
        hideOnSinglePage: false,
        size: 'small',
        showTotal: total => `Found ${total} results.`,
        pageSizeOptions: ['5', '10', '25', '50', '100']
    }
};

const calculatePagination = (pagination) => {
    const newPagination: any = {};

    newPagination.limit = pagination.pageSize;
    newPagination.offset = (pagination.current - 1) * pagination.pageSize;
    newPagination.page = pagination.current;

    return newPagination;
};

export default calculatePagination
