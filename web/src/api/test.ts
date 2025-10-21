import { apiRequest } from './client';

export interface TestAIResponse {
    text: string;
    status: string;
}

export async function testAI() {
    return apiRequest<TestAIResponse>('/api/ai/test');
}