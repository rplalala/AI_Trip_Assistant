package com.demo.api.mapper;

import com.demo.api.dto.TripInsightDTO;
import com.demo.api.model.TripInsight;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TripInsightMapper {
    public TripInsightDTO toDto(TripInsight e) {
        return new TripInsightDTO(
                e.getId().toString(),
                e.getTitle(),
                e.getContent(),
                e.getTheme(),
                e.getIcon()
        );
    }

    public List<TripInsightDTO> toDtoList(List<TripInsight> list){
        return list.stream().map(this::toDto).toList();
    }
}