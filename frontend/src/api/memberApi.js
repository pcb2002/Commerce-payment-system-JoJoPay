import axios from './axios';

export const getMyInfo = async () => {
    // axios가 이미 인터셉터 설정을 통해 jwtToken을 헤더에 실어보내고 있을 것입니다.
    const response = await axios.get('/api/v1/members/me');
    return response.data; // CommonApiResponse 객체 반환
};