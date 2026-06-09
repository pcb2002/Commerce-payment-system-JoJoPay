import axios from './axios';

export const previewOrder = async ({ cartItemIds }) => {
    const response = await axios.post('/api/v1/orders/preview', { cartItemIds });
    return response.data;
};

export const createOrder = async ({ cartItemIds, usedPoint = 0 }) => {
    const response = await axios.post('/api/v1/orders', { cartItemIds, usedPoint });
    return response.data;
};

export const getMyOrders = async ({ page = 0, size = 10 } = {}) => {
    const response = await axios.get('/api/v1/orders', { params: { page, size } });
    return response.data;
};

export const getOrderDetail = async (orderNumber) => {
    const response = await axios.get(`/api/v1/orders/${orderNumber}`);
    return response.data;
};

export const cancelOrder = async (orderNumber) => {
    const response = await axios.post(`/api/v1/orders/${orderNumber}/cancel`);
    return response.data;
};