import { apiRequest } from './client';

export interface LoginPayload {
    email: string;
    password: string;
}

export interface RegisterPayload {
    email: string;
    password: string;
    username: string;
}

export interface DeleteAccountPayload {
    verifyPassword: string;
}

export interface UserProfileResponse {
    username: string;
    email: string;
    avatar: string;
    age: number;
    gender?: number;
}

export async function login(payload: LoginPayload) {
    return apiRequest<string>('/api/login', {
        method: 'POST',
        body: payload,
    });
}

export async function register(payload: RegisterPayload) {
    return apiRequest<string>('/api/register', {
        method: 'POST',
        body: payload,
    });
}

export async function logout() {
    return apiRequest('/api/logout', { method: 'POST' });
}

export async function getUserProfile() {
    return apiRequest<UserProfileResponse>('/api/users/profile');
}

export async function deleteUserAccount(payload: DeleteAccountPayload) {
    return apiRequest<void>('/api/users/profile' , {
        method: 'DELETE',
        body: payload
    });
}

export async function forgotPassword(email: string) {
    return apiRequest<void>(`/api/forgot-password?email=${encodeURIComponent(email)}` , { method: 'POST' });
}

export async function resetPassword(token: string, newPassword: string) {
    return apiRequest<void>(`/api/reset-password?token=${encodeURIComponent(token)}&newPassword=${encodeURIComponent(newPassword)}`, { method: 'POST' });
}

export async function verifyResetPasswordEmail(token: string) {
    return apiRequest<void>(`/api/verify-reset-password-email?token=${encodeURIComponent(token)}`);
}

export async function verifyEmail(token: string) {
    return apiRequest<string>(`/api/verify-email?token=${encodeURIComponent(token)}`);
}

export async function resendVerifyEmail(email: string) {
    return apiRequest<void>(`/api/resend-verify-email?email=${encodeURIComponent(email)}`, { method: 'POST' });
}
