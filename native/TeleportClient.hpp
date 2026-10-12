// SPDX-License-Identifier: GPL-3.0-only
// Copyright (c) 2026 iAlturki — Minecraft AntiServerClose
#pragma once
#include "botcraft/Game/ConnectionClient.hpp"
#include "botcraft/Network/NetworkManager.hpp"
#include <array>
#include <cmath>
#include <cstdio>
#include <stdexcept>

// Minecraft 26.3 / protocol 777 requires the accepted absolute coordinates
// in its teleport acknowledgement. The upstream minimal AFK client omits them.
class TeleportClient : public Botcraft::ConnectionClient {
    std::array<double, 3> position{};
    float yaw = 0, pitch = 0;
    bool known = false;
protected:
    virtual void Emit(const std::shared_ptr<ProtocolCraft::Packet>& packet) {
        GetNetworkManager()->Send(packet);
    }
    void Handle(ProtocolCraft::ClientboundPlayerPositionPacket& packet) override {
        const auto& change = packet.GetChange();
        const auto& requested = change.GetPosition();
        const int flags = packet.GetRelatives();
        if (!known && (flags & 7)) throw std::runtime_error("Initial teleport position must be absolute");
        for (int i = 0; i < 3; ++i) {
            position[i] = requested[i] + ((flags & (1 << i)) ? position[i] : 0);
            if (!std::isfinite(position[i])) throw std::runtime_error("Invalid teleport coordinate");
        }
        yaw = change.GetYRot() + ((flags & 8) ? yaw : 0);
        pitch = change.GetXRot() + ((flags & 16) ? pitch : 0);
        if (!std::isfinite(yaw) || !std::isfinite(pitch)) throw std::runtime_error("Invalid teleport rotation");
        known = true;
        auto reply = std::make_shared<ProtocolCraft::ServerboundAcceptTeleportationPacket>();
        reply->SetId_(packet.GetId_());
        reply->SetX(position[0]); reply->SetY(position[1]); reply->SetZ(position[2]);
        reply->SetYRot(yaw); reply->SetXRot(pitch);
        Emit(reply);
    }
    void Handle(ProtocolCraft::ClientboundRespawnPacket& packet) override {
        known = false; position = {}; yaw = pitch = 0;
        Botcraft::ConnectionClient::Handle(packet);
    }
};
