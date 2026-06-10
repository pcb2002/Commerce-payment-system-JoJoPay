// src/pages/my/MyProfilePage.jsx

import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { getMyInfo } from '../../api/memberApi';

const GRADE_LABEL = {
    BRONZE: { label: 'Bronze', cls: 'text-bg-warning' },
    SILVER: { label: 'Silver', cls: 'text-bg-secondary' },
    GOLD:   { label: 'Gold',   cls: 'text-bg-warning' },
    VIP:    { label: 'VIP',    cls: 'text-bg-danger' },
};

export default function MyProfilePage() {
    const navigate = useNavigate();
    const [info, setInfo] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    useEffect(() => {
        getMyInfo()
            .then((res) => setInfo(res.data))
            .catch(() => setError('회원 정보를 불러오는 데 실패했습니다.'))
            .finally(() => setLoading(false));
    }, []);

    if (loading) return (
        <div className="d-flex justify-content-center align-items-center" style={{ minHeight: '50vh' }}>
            <div className="spinner-border text-primary" role="status" />
        </div>
    );

    if (error) return (
        <div className="container py-4">
            <div className="alert alert-danger">{error}</div>
        </div>
    );

    const grade = GRADE_LABEL[info.membershipGrade] || { label: info.membershipGrade, cls: 'text-bg-light' };

    return (
        <div className="container py-4" style={{ maxWidth: '560px' }}>
            <button className="btn btn-outline-secondary btn-sm mb-3" onClick={() => navigate(-1)}>← 뒤로가기</button>
            <h2 className="h5 fw-semibold mb-4">내 정보</h2>

            <div className="card mb-3">
                <div className="card-body">
                    <div className="d-flex align-items-center gap-3 mb-3">
                        <div
                            className="rounded-circle bg-primary text-white d-flex align-items-center justify-content-center fw-semibold"
                            style={{ width: 52, height: 52, fontSize: 20 }}
                        >
                            {info.name?.slice(0, 1).toUpperCase()}
                        </div>
                        <div>
                            <p className="fw-semibold mb-0">{info.name}</p>
                            <p className="text-muted small mb-0">{info.email}</p>
                        </div>
                        <span className={`badge ms-auto ${grade.cls}`}>{grade.label}</span>
                    </div>
                    <hr />
                    <table className="table table-borderless mb-0 small">
                        <tbody>
                        <tr>
                            <td className="text-muted ps-0" style={{ width: 140 }}>전화번호</td>
                            <td>{info.phoneNumber}</td>
                        </tr>
                        <tr>
                            <td className="text-muted ps-0">포인트 잔액</td>
                            <td className="fw-medium">{info.pointBalance?.toLocaleString()}P</td>
                        </tr>
                        <tr>
                            <td className="text-muted ps-0">총 결제 금액</td>
                            <td className="fw-medium">{info.totalPaymentAmount?.toLocaleString()}원</td>
                        </tr>
                        <tr>
                            <td className="text-muted ps-0">가입일</td>
                            <td>{new Date(info.createdAt).toLocaleDateString('ko-KR')}</td>
                        </tr>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    );
}