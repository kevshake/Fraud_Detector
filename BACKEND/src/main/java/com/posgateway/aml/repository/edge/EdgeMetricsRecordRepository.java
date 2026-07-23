package com.posgateway.aml.repository.edge;

import com.posgateway.aml.entity.edge.EdgeMetricsRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EdgeMetricsRecordRepository extends JpaRepository<EdgeMetricsRecord, Long> {

    List<EdgeMetricsRecord> findTop20ByEdgeNodeIdOrderByWindowEndDesc(Long edgeNodeId);

    List<EdgeMetricsRecord> findTop50ByPspIdOrderByWindowEndDesc(Long pspId);
}
