// SPDX-License-Identifier: GPL-3.0-only
// Copyright (c) 2026 iAlturki — Minecraft AntiServerClose
#include "TeleportClient.hpp"
#include <iomanip>
#include <iostream>

class CheckClient : public TeleportClient {
public:
    using TeleportClient::Handle;
    std::shared_ptr<ProtocolCraft::Packet> sent;
protected:
    void Emit(const std::shared_ptr<ProtocolCraft::Packet>& p) override { sent = p; }
};

static void sample(CheckClient& client, int id, std::array<double,3> pos, float yaw, float pitch, int flags) {
    ProtocolCraft::PositionMoveRotation change;
    change.SetPosition(pos); change.SetDeltaMovement({0,0,0});
    change.SetYRot(yaw); change.SetXRot(pitch);
    ProtocolCraft::ClientboundPlayerPositionPacket incoming;
    incoming.SetId_(id); incoming.SetChange(change); incoming.SetRelatives(flags);
    client.Handle(incoming);
    ProtocolCraft::WriteContainer bytes;
    client.sent->Write(bytes);
    std::cout << "ACK_HEX ";
    for (auto b : bytes) std::cout << std::hex << std::setfill('0') << std::setw(2) << static_cast<int>(b);
    std::cout << '\n';
}
int main() {
    CheckClient client;
    sample(client, 1, {120.25,63,-75.5}, 45, 10, 0);
    sample(client, 2, {1.5,70,-2}, 15, -20, 1|4|8|16);
    sample(client, 3, {-100,5,100}, -90, 0, 2);
    bool rejected = false;
    try { CheckClient fresh; sample(fresh,4,{1,2,3},0,0,1); } catch (const std::runtime_error&) { rejected = true; }
    if (!rejected) throw std::runtime_error("Unknown relative initial position was accepted");
    return 0;
}
