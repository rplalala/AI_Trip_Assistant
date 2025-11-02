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
  Checkbox,
  Modal,
  message,
} from "antd";
import type { DefaultOptionType } from "antd/es/select";
import { PlusOutlined, DeleteOutlined } from "@ant-design/icons";
import { useNavigate } from "react-router-dom";
import { getTripDetails, type TripDetail, deleteTrips } from "../../api/trip";

const { Title, Text } = Typography;

// Types
type StatusFilter = "All" | "Upcoming" | "In Progress" | "Past";

type Option<T extends string> = {
  value: T;
  label: string;
};

// Options
const statusOptions: Option<StatusFilter>[] = [
  { value: "All", label: "All" },
  { value: "Upcoming", label: "Upcoming" },
  { value: "In Progress", label: "In Progress" },
  { value: "Past", label: "Past" },
];

export default function TripsPage(): React.ReactElement {
  const navigate = useNavigate();

  // state
  const [loading, setLoading] = useState<boolean>(false);
  const [trips, setTrips] = useState<TripDetail[]>([]);
  const [status, setStatus] = useState<StatusFilter>("All");
  const [selectMode, setSelectMode] = useState<boolean>(false);
  const [selected, setSelected] = useState<Set<number>>(new Set());
  const [deleting, setDeleting] = useState<boolean>(false);

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
      const start = t.startDate;
      const end = t.endDate;
      if (status === "Upcoming") return start > today;
      if (status === "In Progress") return start <= today && end >= today;
      if (status === "Past") return end < today;
      return true;
    });
  }, [trips, status]);

  const isAllSelected =
    filteredTrips.length > 0 &&
    filteredTrips.every((t) => selected.has(t.tripId));
  const hasSelection = selected.size > 0;

  const toggleSelect = (tripId: number) => {
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(tripId)) next.delete(tripId);
      else next.add(tripId);
      return next;
    });
  };

  const handleCardClick = (tripId: number) => {
    if (selectMode) {
      toggleSelect(tripId);
      return;
    }
    navigate(`/trips/${tripId}`);
  };

  const handleSelectAll = () => {
    if (isAllSelected) {
      // clear only those in current filtered list
      setSelected((prev) => {
        const next = new Set(prev);
        for (const t of filteredTrips) next.delete(t.tripId);
        return next;
      });
    } else {
      setSelected((prev) => {
        const next = new Set(prev);
        for (const t of filteredTrips) next.add(t.tripId);
        return next;
      });
    }
  };

  const handleClearSelection = () => setSelected(new Set());

  const confirmAndDelete = () => {
    if (!hasSelection) return;
    Modal.confirm({
      title: "Delete selected trips?",
      content: "This action cannot be undone.",
      okText: "Delete",
      okButtonProps: { danger: true },
      cancelText: "Cancel",
      onOk: async () => {
        try {
          setDeleting(true);
          const ids = Array.from(selected);
          await deleteTrips(ids);
          setTrips((prev) => prev.filter((t) => !selected.has(t.tripId)));
          setSelected(new Set());
          setSelectMode(false);
          message.success("Deleted successfully");
        } catch (e: unknown) {
          console.error(e);
          const errMsg = e instanceof Error ? e.message : String(e);
          message.error(errMsg || "Failed to delete");
        } finally {
          setDeleting(false);
        }
      },
    });
  };

  return (
    <Space direction="vertical" size="large" style={{ width: "100%" }}>
      {/* Breadcrumb */}
      <Breadcrumb items={[{ title: "Home", href: "/" }, { title: "Trips" }]} />

      {/* Header */}
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

        <Space>
          {selectMode ? (
            <>
              <Button
                onClick={handleSelectAll}
                disabled={filteredTrips.length === 0}
              >
                {isAllSelected ? "Unselect All" : "Select All"}
              </Button>
              <Button onClick={handleClearSelection} disabled={!hasSelection}>
                Clear
              </Button>
              <Button
                type="primary"
                danger
                icon={<DeleteOutlined />}
                disabled={!hasSelection}
                loading={deleting}
                onClick={confirmAndDelete}
              >
                Delete Selected
              </Button>
              <Button onClick={() => setSelectMode(false)}>Cancel</Button>
            </>
          ) : (
            <>
              <Button
                type="primary"
                icon={<PlusOutlined />}
                onClick={() => navigate("/trips/new")}
              >
                Create Trip
              </Button>
              <Button onClick={() => setSelectMode(true)}>Manage</Button>
            </>
          )}
        </Space>
      </div>

      {/* Filters */}
      <Space size="middle" wrap>
        <Select<StatusFilter, DefaultOptionType>
          value={status}
          onChange={(v) => setStatus(v)}
          options={statusOptions}
          style={{ width: 120 }}
        />
        {selectMode && <Text type="secondary">Selected: {selected.size}</Text>}
      </Space>

      {/* Content */}
      {loading ? (
        <Row gutter={[24, 24]}>
          {Array.from({ length: 6 }).map((_, idx) => (
            <Col key={idx} xs={24} sm={12} lg={8}>
              <Card bodyStyle={{ padding: 14 }} style={{ borderRadius: 12, overflow: "hidden" }}>
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
            const checked = selected.has(t.tripId);

            return (
              <Col key={t.tripId} xs={24} sm={12} lg={8}>
                <div style={{ position: "relative" }}>
                  {selectMode && (
                    <Checkbox
                      checked={checked}
                      onChange={() => toggleSelect(t.tripId)}
                      style={{ position: "absolute", bottom: 12, right: 12, zIndex: 2 }}
                      onClick={(e) => e.stopPropagation()}
                    />
                  )}
                  <Card
                    hoverable
                    onClick={() => handleCardClick(t.tripId)}
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
                        style={{ width: "100%", height: 200, objectFit: "cover" }}
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
                </div>
              </Col>
            );
          })}
        </Row>
      )}
    </Space>
  );
}
