import axios from './axios';

export const getProducts = async (params) => {
    const response = await axios.get('/api/v1/products', { params });
    return response.data;
};

export const getProductDetail = async (productId) => {
    const response = await axios.get(`/api/v1/products/${productId}`);
    return response.data;
};