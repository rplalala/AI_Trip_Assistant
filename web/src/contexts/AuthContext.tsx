/* eslint-disable react-refresh/only-export-components */

import * as React from 'react'
import { createContext, useContext, useEffect, useState } from 'react';
import { getUserProfile } from '../api/user.ts';

export type Status = 'loading' | 'authenticated' | 'unauthenticated';

interface User {
    username: string;
    email: string;
    avatar: string;
    age: number;
    gender?: number;
}

type AuthContextValue = {
    status: Status;
    user: User | null;
    setUser: React.Dispatch<React.SetStateAction<User | null>>;
    setStatus: React.Dispatch<React.SetStateAction<Status>>;
    refreshProfile: () => Promise<void>;
};

const AuthCtx = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
    const [status, setStatus] = useState<Status>('loading');
    const [user, setUser] = useState<User | null>(null);

    const refreshProfile = async () => {
        const user = await getUserProfile();
        setUser(user);
        setStatus('authenticated');
    }

    useEffect(() => {
        refreshProfile()
            .catch(() => setStatus('unauthenticated'));
    }, []);

    return (
        <AuthCtx.Provider value={{ status, user, setUser, setStatus, refreshProfile }}>
            {children}
        </AuthCtx.Provider>
    )
}

export function useAuth(): AuthContextValue {
    const ctx = useContext(AuthCtx);
    if (!ctx) {
        throw new Error('useAuth must be used within <AuthProvider>');
    }
    return ctx;
}