package com.ds.communication_service.common.interfaces;

import java.util.UUID;

import com.ds.communication_service.app_context.models.InteractiveProposal;
import com.ds.communication_service.common.dto.CreateProposalRequest;

public interface IProposalService {

    /**
     * Tạo một proposal mới và gửi tin nhắn.
     * @param dto Dữ liệu để tạo proposal.
     * @param senderId ID của người gửi (đã xác thực).
     * @param senderRoles Danh sách role của người gửi.
     * @return Proposal đã được lưu.
     */
    InteractiveProposal createProposal(CreateProposalRequest dto);

    /**
     * HÀM MỚI (Thay thế cho accept/decline)
     * Người nhận (recipient) phản hồi một proposal với dữ liệu.
     *
     * @param proposalId ID của proposal.
     * @param currentUserId ID của người dùng đang thực hiện (phải là recipient).
     * @param resultData Dữ liệu phản hồi (ví dụ: "ACCEPTED", "lý do...", "ngày...")
     * @return Proposal đã được cập nhật.
     */
    InteractiveProposal respondToProposal(UUID proposalId, String currentUserId, String resultData);
}