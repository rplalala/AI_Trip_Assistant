import { Alert, App as AntdApp, Button } from 'antd';
import { useEffect, useMemo, useRef, useState } from 'react';
import { verifyEmail } from '../../api/user';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';

export default function VerifyEmailPage() {
    const [params] = useSearchParams();
    const token = useMemo(() => params.get('token') || '', [params]);
    const { message } = AntdApp.useApp();
    const [status, setStatus] = useState<'pending' | 'success' | 'fail'>('pending');
    const navigate = useNavigate();
    const calledRef = useRef(false);
    const { setStatus: setAuthStatus, refreshProfile } = useAuth();

    useEffect(() => {
        if (calledRef.current) return;
        calledRef.current = true;

        if (!token) {
            setStatus('fail');
            return;
        }
        verifyEmail(token)
            .then(async (jwt: string) => {
                localStorage.setItem('token', jwt);
                setAuthStatus('authenticated');
                setStatus('success');
                refreshProfile()
                    .then(() => {
                        setTimeout(() => navigate('/trips', { replace: true }), 1200);
                    })
            })
            .catch((err: unknown) => {
                const msg = err instanceof Error ? err.message : 'Verification failed.';
                message.error(msg);
                setStatus('fail');
            });
    }, [token, message, navigate, setAuthStatus]);

    let content: React.ReactNode;
    if (status === 'success') {
        content = (
            <Alert type="success" showIcon message="Email verified successfully! You're now signed in." />
        );
    } else if (status === 'fail') {
        content = (
            <>
                <Alert
                    type="error"
                    showIcon
                    message="Verification failed. The link may be invalid or expired."
                    style={{ marginBottom: 16 }}
                />
                <Button type="primary" block onClick={() => navigate('/login')}>Back to Login</Button>
            </>
        );
    } else {
        content = <Alert type="info" showIcon message="Verifying..." />;
    }

    return (
        <div style={{ minHeight: '80vh', padding: 24, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <div style={{ width: 520 }}>
                <h3 style={{ textAlign: 'left', marginBottom: 16 }}>Email Verification</h3>
                {content}
            </div>
        </div>
    );
}
