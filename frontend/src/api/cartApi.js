import axios from './axios';

export const getCart = async () => {
    const response = await axios.get('/api/V1/cart');
    return response.data;
};

export const addCartItem = async ({ productId, quantity }) => {
    const response = await axios.post('/api/V1/cart/items', { productId, quantity });
    return response.data;
};

export const updateCartItemQuantity = async (cartItemId, quantity) => {
    const response = await axios.put(`/api/V1/cart/items/${cartItemId}`, { quantity });
    return response.data;
};

export const deleteCartItem = async (cartItemId) => {
    const response = await axios.delete(`/api/V1/cart/items/${cartItemId}`);
    return response.data;
};

export const clearCart = async () => {
    const response = await axios.delete('/api/V1/cart');
    return response.data;
};