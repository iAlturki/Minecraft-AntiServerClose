// SPDX-License-Identifier: GPL-3.0-only
// Copyright (c) 2026 iAlturki — Minecraft AntiServerClose
// Uses the GPL-3.0 Botcraft client library; see NOTICE and THIRD_PARTY.md.
#include "TeleportClient.hpp"
#include "botcraft/Utilities/Logger.hpp"
#include <algorithm>
#include <chrono>
#include <iostream>
#include <string>
#include <thread>

static void help() {
    std::cout << "Minecraft AntiServerClose by iAlturki — client for Minecraft 26.3\n"
              << "Usage: AntiServerCloseClient --address HOST:PORT --expected-username NAME\n"
              << "  --help                Show this message; no authentication or connection\n"
              << "  --address             Your server's explicit hostname/IPv4 and port\n"
              << "  --expected-username   Must match the licensed Microsoft account you approve\n"
              << "Microsoft device authentication only. Credentials stay in the working directory.\n";
}

static std::string lower(std::string value) {
    for (char& c : value) if (c >= 'A' && c <= 'Z') c += 'a' - 'A';
    return value;
}

int main(int argc, char** argv) {
    std::string address, expected;
    for (int i = 1; i < argc; ++i) {
        const std::string arg = argv[i];
        if ((arg == "--help" || arg == "-h") && argc == 2) { help(); return 0; }
        if ((arg != "--address" && arg != "--expected-username") || i + 1 >= argc) {
            std::cerr << "Invalid or incomplete arguments. Run --help.\n"; return 2;
        }
        (arg == "--address" ? address : expected) = argv[++i];
    }
    if (address.empty() || expected.size() < 3 || expected.size() > 16 ||
        !std::all_of(expected.begin(), expected.end(), [](char c) {
            return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_';
        })) { std::cerr << "Address and valid expected username are required.\n"; return 2; }

    try {
        auto& logger = Botcraft::Logger::GetInstance();
        logger.SetLogLevel(Botcraft::LogLevel::Info);
        logger.SetFilename("");
        logger.RegisterThread("iAlturki-AntiServerClose");
        TeleportClient client;
        client.ConnectMicrosoft(address);
        if (lower(client.GetNetworkManager()->GetMyName()) != lower(expected)) {
            std::cerr << "Authenticated account does not match configured username.\n";
            client.Disconnect(); return 42;
        }
        // The Fabric supervisor owns shutdown and disconnect recovery. Avoid
        // reading Botcraft's non-atomic close flag from another thread here.
        for (;;) std::this_thread::sleep_for(std::chrono::milliseconds(100));
    } catch (const std::exception&) {
        std::cerr << "Client failed; inspect the private server log for the preceding reason.\n";
        return 1;
    }
}
