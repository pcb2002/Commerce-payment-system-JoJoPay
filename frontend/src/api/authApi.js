import axios from './axios';

export const login = async ({ email, password }) => {
    const response = await axios.post('/api/v1/auth/login', { email, password });
    return response.data;
};

export const signup = async ({ email, password, name, phoneNumber }) => {
    const response = await axios.post('/api/v1/auth/signup', { email, password, name, phoneNumber });
    return response.data;
};