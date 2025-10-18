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