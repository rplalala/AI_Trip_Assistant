import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import type { JSX } from 'react'

export default function RequireAuth({ children }: { children: JSX.Element }) {
    const { status } = useAuth();
    const location = useLocation();

    if (status === 'loading') return null;
    if (status === 'unauthenticated') {
        return <Navigate to="/login" state={{ from: location }} replace />;
    }
    return children;
}