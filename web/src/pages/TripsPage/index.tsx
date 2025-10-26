import React, { useEffect, useMemo, useState } from "react";
import {
  Breadcrumb,
  Typography,
  Space,
  Button,
  Select,
  Row,
  Col,
  Card,
  Empty,
  Skeleton,
} from "antd";
import type { DefaultOptionType } from "antd/es/select";
import { PlusOutlined } from "@ant-design/icons";
import { useNavigate } from "react-router-dom";
import { getTripDetails, type TripDetail } from "../../api/trip";

const { Title, Text } = Typography;

// ---- 类型定义 ----
// Filters
type StatusFilter = "All" | "Upcoming" | "Past";

 type Option<T extends string> = {
   value: T;
   label: string;
 };

 // ---- 下拉选项（带类型）----
 const statusOptions: Option<StatusFilter>[] = [
   { value: "All", label: "All" },
   { value: "Upcoming", label: "Upcoming" },
   { value: "Past", label: "Past" },
 ];

export default function TripsPage(): React.ReactElement {
  const navigate = useNavigate();

  // state
  const [loading, setLoading] = useState<boolean>(false);
  const [trips, setTrips] = useState<TripDetail[]>([]);
  const [status, setStatus] = useState<StatusFilter>("All");

  // fetch trips
  useEffect(() => {
    let mounted = true;
    setLoading(true);
    getTripDetails()
      .then((list) => {
        if (!mounted) return;
        setTrips(list ?? []);
      })
      .catch((err) => {
        console.error("Failed to load trips", err);
        if (mounted) setTrips([]);
      })
      .finally(() => mounted && setLoading(false));
    return () => {
      mounted = false;
    };
  }, []);

  // derive filtered trips based on start/end date vs today
  const filteredTrips = useMemo(() => {
    if (status === "All") return trips;
    const today = new Date().toISOString().slice(0, 10);
    return trips.filter((t) => {
      const end = t.endDate;
      if (status === "Upcoming") return end >= today;
      if (status === "Past") return end < today;
      return true;
    });
  }, [trips, status]);

  return (
    <Space direction="vertical" size="large" style={{ width: "100%" }}>
      {/* 面包屑 */}
      <Breadcrumb items={[{ title: "Home" }, { title: "Trips" }]} />

      {/* 标题 + 按钮 */}
      <div
        style={{
          display: "flex",
          alignItems: "center",
          justifyContent: "space-between",
          gap: 12,
        }}
      >
        <Title level={1} style={{ margin: 0, fontWeight: 700 }}>
          My Trips
        </Title>

        <Button
          type="default"
          icon={<PlusOutlined />}
          onClick={() => navigate("/trips/new")}
        >
          Create Trip
        </Button>
      </div>

      {/* 筛选条 */}
      <Space size="middle" wrap>
        <Select<StatusFilter, DefaultOptionType>
          value={status}
          onChange={(v) => setStatus(v)}
          options={statusOptions}
          style={{ width: 120 }}
        />
      </Space>

      {/* 内容 */}
      {loading ? (
        <Row gutter={[24, 24]}>
          {Array.from({ length: 6 }).map((_, idx) => (
            <Col key={idx} xs={24} sm={12} lg={8}>
              <Card
                bodyStyle={{ padding: 14 }}
                style={{ borderRadius: 12, overflow: "hidden" }}
              >
                <Skeleton.Image style={{ width: "100%", height: 200 }} active />
                <Skeleton active title paragraph={{ rows: 2 }} style={{ marginTop: 12 }} />
              </Card>
            </Col>
          ))}
        </Row>
      ) : filteredTrips.length === 0 ? (
        <Empty description="No trips yet" />
      ) : (
        <Row gutter={[24, 24]}>
          {filteredTrips.map((t) => {
            const title = `${t.toCity || t.toCountry || "Trip"}`;
            const dateText = `${t.startDate} – ${t.endDate}`;
            const cover =
              t.imgUrl ||
              `https://source.unsplash.com/featured/800x600?${encodeURIComponent(t.toCity || t.toCountry || "travel")}`;

            return (
              <Col key={t.tripId} xs={24} sm={12} lg={8}>
                <Card
                  hoverable
                  onClick={() => navigate(`/trips/${t.tripId}`)}
                  bodyStyle={{ padding: 14 }}
                  style={{
                    borderRadius: 12,
                    overflow: "hidden",
                    boxShadow: "0 2px 6px rgba(0,0,0,0.06)",
                  }}
                  cover={
                    <img
                      alt={title}
                      src={cover}
                      style={{
                        width: "100%",
                        height: 200,
                        objectFit: "cover",
                      }}
                    />
                  }
                >
                  <div style={{ display: "flex", flexDirection: "column", gap: 6 }}>
                    <Text strong style={{ fontSize: 16 }}>
                      {title}
                    </Text>
                    <Text type="secondary">
                      {dateText} · {t.people} travelers · Budget: {" "}
                      <Text underline>${t.budget?.toLocaleString?.() ?? t.budget}</Text>
                    </Text>
                  </div>
                </Card>
              </Col>
            );
          })}
        </Row>
      )}
    </Space>
  );
}
