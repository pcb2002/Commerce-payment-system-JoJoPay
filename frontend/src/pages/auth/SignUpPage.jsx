import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { signup } from '../../api/authApi';

export default function SignupPage() {
    const navigate = useNavigate();
    const [form, setForm] = useState({ name: '', email: '', password: '', phoneNumber: '' });
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);

    const handleChange = (e) => {
        setForm({ ...form, [e.target.name]: e.target.value });
        setError('');
    };

    const validate = () => {
        if (!form.name) return '이름을 입력해주세요.';
        if (!form.email) return '이메일을 입력해주세요.';
        if (!form.password) return '비밀번호를 입력해주세요.';
        if (form.password.length < 8 || form.password.length > 20)
            return '비밀번호는 8자 이상 20자 이하로 입력해주세요.';
        if (!form.phoneNumber) return '휴대폰 번호를 입력해주세요.';
        const phoneRegex = /^01[0-9]-?\d{3,4}-?\d{4}$/;
        if (!phoneRegex.test(form.phoneNumber)) return '올바른 휴대폰 번호 형식이 아닙니다. (예: 010-1234-5678)';
        return null;
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        const validationError = validate();
        if (validationError) return setError(validationError);

        setLoading(true);
        try {
            await signup(form);
            navigate('/login', { state: { message: '회원가입이 완료되었습니다. 로그인해주세요.' } });
        } catch (err) {
            setError(err.response?.data?.message || '회원가입에 실패했습니다.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="min-vh-100 d-flex align-items-center justify-content-center bg-light py-4">
            <div className="card shadow-sm" style={{ width: '100%', maxWidth: '420px' }}>
                <div className="card-body p-4">
                    <div className="text-center mb-4">
                        <h1 className="h4 fw-semibold mb-1">JojoPay</h1>
                        <p className="text-muted small">새 계정을 만드세요</p>
                    </div>

                    {error && (
                        <div className="alert alert-danger py-2 small">{error}</div>
                    )}

                    <form onSubmit={handleSubmit}>
                        <div className="mb-3">
                            <label className="form-label small text-muted">이름</label>
                            <input
                                type="text"
                                name="name"
                                className="form-control"
                                placeholder="홍길동"
                                value={form.name}
                                onChange={handleChange}
                            />
                        </div>

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
                                placeholder="8~20자"
                                value={form.password}
                                onChange={handleChange}
                            />
                            <div className="form-text">8자 이상 20자 이하로 입력해주세요.</div>
                        </div>

                        <div className="mb-3">
                            <label className="form-label small text-muted">휴대폰 번호</label>
                            <input
                                type="text"
                                name="phoneNumber"
                                className="form-control"
                                placeholder="010-1234-5678"
                                value={form.phoneNumber}
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
                            회원가입
                        </button>
                    </form>

                    <p className="text-center small text-muted mt-3 mb-0">
                        이미 계정이 있으신가요?{' '}
                        <Link to="/login" className="text-primary text-decoration-none">
                            로그인
                        </Link>
                    </p>
                </div>
            </div>
        </div>
    );
}