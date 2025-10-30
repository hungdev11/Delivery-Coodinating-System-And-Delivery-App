package com.ds.deliveryapp.clients.res;

import com.ds.deliveryapp.model.DeliveryAssignment;

import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DeliverySession {
    private UUID id;
    private String deliveryManId;
    private String status;
    private String startTime;
    private String endTime;
    private int totalTasks;
    private int completedTasks;
    private int failedTasks;
    private List<DeliveryAssignment> assignments;
}
