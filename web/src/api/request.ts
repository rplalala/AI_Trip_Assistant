export async function request<T>(url: string, options?: RequestInit): Promise<T> {
    const res = await fetch(url, {
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        ...options,
    });

    if (!res.ok) {
        const msg = await res.text();
        throw new Error(`HTTP ${res.status}: ${msg}`);
    }

    return await res.json() as Promise<T>;
}