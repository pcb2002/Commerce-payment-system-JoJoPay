import axios from './axios';

export const getMyRefunds = async () => {
    const response = await axios.get('/api/v1/orders/my');
    return response.data;
};

export const refundOrder = async (orderId, { orderNumber, reason, items }) => {
    const response = await axios.post(`/api/v1/orders/${orderId}/refund`, { orderNumber, reason, items });
    return response.data;
};