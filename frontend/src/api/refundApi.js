import api from "./axios.js";

export const refundOrder = (orderId, orderNumber, reason, items) =>
    api.post(`/api/v1/orders/${orderId}/refund`, { orderNumber, reason, items });