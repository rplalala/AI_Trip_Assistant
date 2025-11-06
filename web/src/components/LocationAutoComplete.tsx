import { useEffect, useRef, useState } from 'react'
import { AutoComplete } from 'antd'

type LocationType = 'city' | 'country';

export type Location = {
    id: string;
    type: LocationType;
    name: string;
    country: { code: string; name: string } | null;
    lat?: number;
    lng?: number;
    timezone?: string;
};

type GeoCity = {
    id: number;
    city: string;
    country: string;
    countryCode: string;
    latitude: number;
    longitude: number;
    timezone: string;
    population: number;
};

type GeoCountry = {
    code: string;
    name: string;
};

// const CITIES_URL = 'http://geodb-free-service.wirefreethought.com/v1/geo/cities';
const CITIES_URL    = '/ext/geodb/cities';
const COUNTRIES_URL = '/ext/geodb/countries';
// const COUNTRIES_URL = 'http://geodb-free-service.wirefreethought.com/v1/geo/countries';

async function searchGeo(query: string, signal?: AbortSignal): Promise<Location[]> {
    if (!query) return [];
    const limit = 6;

    const [citiesRes, countriesRes] = await Promise.all([
        fetch(`${CITIES_URL}?limit=${limit}&sort=-population&namePrefix=${encodeURIComponent(query)}`, { signal }),
        fetch(`${COUNTRIES_URL}?limit=4&namePrefix=${encodeURIComponent(query)}`, { signal }),
    ]);

    const [citiesJson, countriesJson] = await Promise.all([citiesRes.json(), countriesRes.json()]);

    const cities: Location[] = (citiesJson.data as GeoCity[]).map((c) => ({
        id: `city:${c.id}`,
        type: 'city',
        name: c.city,
        country: { code: c.countryCode, name: c.country },
        lat: c.latitude,
        lng: c.longitude,
        timezone: c.timezone,
    }));

    const countries: Location[] = (countriesJson.data as GeoCountry[]).map((c) => ({
        id: `country:${c.code}`,
        type: 'country',
        name: c.name,
        country: { code: c.code, name: c.name },
    }));

    return [...cities, ...countries];
}

type LocationAutoCompleteProps = {
    label?: string;
    placeholder?: string;
    value?: Location | null;
    onChange?: (loc: Location | null) => void;
    allowClear?: boolean;
};

function formatLabel(loc: Location) {
    return loc.type === 'city'
        ? `${loc.name}, ${loc.country?.name}`
        : `${loc.name} (country)`;
}

export const LocationAutoComplete = ({
                                  placeholder,
                                  value,
                                  onChange,
                                  allowClear = true,
                              }: LocationAutoCompleteProps)=> {
    const [options, setOptions] = useState<{ value: string; label: React.ReactNode }[]>([]);
    const [inputValue, setInputValue] = useState('');
    const abortRef = useRef<AbortController | null>(null);
    const timerRef = useRef<number | undefined>(undefined);

    // 外部 value 变化时，同步展示文案
    useEffect(() => {
        setInputValue(value ? formatLabel(value) : '');
    }, [value?.id]); // 用 id 保证稳定

    const handleSearch = (text: string) => {
        setInputValue(text);
        window.clearTimeout(timerRef.current);
        if (!text) {
            setOptions([]);
            return;
        }
        timerRef.current = window.setTimeout(async () => {
            try {
                abortRef.current?.abort();
                abortRef.current = new AbortController();
                const results = await searchGeo(text, abortRef.current.signal);
                setOptions(
                    results.map((loc) => ({
                        value: JSON.stringify(loc),
                        label: formatLabel(loc),
                    }))
                );
            } catch {}
        }, 300);
    };

    const handleSelect = (val: string) => {
        const loc: Location = JSON.parse(val);
        onChange?.(loc);
        setInputValue(formatLabel(loc));
        setOptions([]);
    };

    const handleChange = (text: string) => {
        setInputValue(text);
        if (!text) onChange?.(null);
    };

    return (
        <AutoComplete
            allowClear={allowClear}
            value={inputValue}
            options={options}
            onSearch={handleSearch}
            onSelect={handleSelect}
            onChange={handleChange}
            placeholder={placeholder}
            style={{ width: '100%' }}
            filterOption={false}
        />
    );
}
