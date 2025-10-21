type HttpMethod = 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH';

interface RequestOptions extends RequestInit {
    method?: HttpMethod;
    body?: any;
    headers?: Record<string, string>;
}

type ApiResp<T> = {
    code: number;    // 1: success
    msg: string;
    data: T;
};

export async function apiRequest<T>(url: string, options: RequestOptions = {}): Promise<T> {
    const { method = 'GET', body, headers = {}, ...rest } = options;
    const token = localStorage.getItem('token');
    if (token) headers.Authorization = `Bearer ${token}`;

    const response = await fetch(url, {
        method,
        headers: {
            'Content-Type': 'application/json',
            ...headers,
        },
        body: body ? JSON.stringify(body) : undefined,
        credentials: 'include',
        ...rest,
    });

    if (!response.ok) {
        let errorMessage = response.statusText;
        try {
            const data: ApiResp<T> = await response.json();
            errorMessage = data.msg || JSON.stringify(data);
        } catch {
            // ignore
        }
        throw new Error(`HTTP ${response.status}: ${errorMessage}`);
    }

    if (response.status === 204) return {} as T;

    const json = (await response.json()) as ApiResp<T> | T;

    if (isApiResp(json)) {
        if (json.code === 1) return json.data;
        throw new Error(json.msg || 'API error');
    }

    return await response.json() as Promise<T>;
}

function isApiResp<T>(v: any): v is ApiResp<T> {
    return v && typeof v === 'object' && 'code' in v && 'msg' in v && 'data' in v;
}