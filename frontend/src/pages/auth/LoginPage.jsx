import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { login } from '../../api/authApi';

export default function LoginPage() {
    const navigate = useNavigate();
    const [form, setForm] = useState({ email: '', password: '' });
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);

    const handleChange = (e) => {
        setForm({ ...form, [e.target.name]: e.target.value });
        setError('');
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!form.email) return setError('이메일을 입력해주세요.');
        if (!form.password) return setError('비밀번호를 입력해주세요.');

        setLoading(true);
        try {
            const data = await login(form);
            localStorage.setItem('jwtToken', data.data.jwtToken);
            navigate('/products');
        } catch (err) {
            setError(err.response?.data?.message || '로그인에 실패했습니다.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="min-vh-100 d-flex align-items-center justify-content-center bg-light">
            <div className="card shadow-sm" style={{ width: '100%', maxWidth: '420px' }}>
                <div className="card-body p-4">
                    <div className="text-center mb-4">
                        <h1 className="h4 fw-semibold mb-1">JojoPay</h1>
                        <p className="text-muted small">계정에 로그인하세요</p>
                    </div>

                    {error && (
                        <div className="alert alert-danger py-2 small">{error}</div>
                    )}

                    <form onSubmit={handleSubmit}>
                        <div className="mb-3">
                            <label className="form-label small text-muted">이메일</label>
                            <input
                                type="email"
                                name="email"
                                className="form-control"
                                placeholder="example@email.com"
                                value={form.email}
                                onChange={handleChange}
                            />
                        </div>

                        <div className="mb-3">
                            <label className="form-label small text-muted">비밀번호</label>
                            <input
                                type="password"
                                name="password"
                                className="form-control"
                                placeholder="비밀번호 입력"
                                value={form.password}
                                onChange={handleChange}
                            />
                        </div>

                        <button
                            type="submit"
                            className="btn btn-primary w-100 mt-1"
                            disabled={loading}
                        >
                            {loading ? (
                                <span className="spinner-border spinner-border-sm me-2" role="status" />
                            ) : null}
                            로그인
                        </button>
                    </form>

                    <p className="text-center small text-muted mt-3 mb-0">
                        계정이 없으신가요?{' '}
                        <Link to="/signup" className="text-primary text-decoration-none">
                            회원가입
                        </Link>
                    </p>
                </div>
            </div>
        </div>
    );
}