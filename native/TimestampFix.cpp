// SPDX-License-Identifier: GPL-3.0-only
// Copyright (c) 2026 iAlturki — Minecraft AntiServerClose
// Corrects certificate expiry conversion in the pinned Botcraft 26.3 library.
// The C++ signature intentionally matches its exported function. No keys or
// tokens are read, altered, logged, or sent by this library.
#include <chrono>
#include <cstdio>
#include <stdexcept>
#include <string>

namespace {
int digits(const std::string& s, std::size_t at, std::size_t count) {
    if (at + count > s.size()) throw std::invalid_argument("short ISO timestamp");
    int out = 0;
    for (std::size_t i = at; i < at + count; ++i) {
        if (s[i] < '0' || s[i] > '9') throw std::invalid_argument("non-digit in ISO timestamp");
        out = out * 10 + (s[i] - '0');
    }
    return out;
}
long long parse(const std::string& s) {
    if (s.size() < 20 || s[4] != '-' || s[7] != '-' || s[10] != 'T' || s[13] != ':' || s[16] != ':')
        throw std::invalid_argument("invalid ISO timestamp shape");
    std::chrono::year_month_day day{std::chrono::year{digits(s, 0, 4)},
        std::chrono::month{static_cast<unsigned>(digits(s, 5, 2))},
        std::chrono::day{static_cast<unsigned>(digits(s, 8, 2))}};
    const int hour = digits(s, 11, 2), minute = digits(s, 14, 2), second = digits(s, 17, 2);
    if (!day.ok() || hour > 23 || minute > 59 || second > 59)
        throw std::invalid_argument("invalid ISO calendar value");
    std::size_t pos = 19;
    int milliseconds = 0, fractionDigits = 0;
    if (s[pos] == '.') {
        ++pos;
        while (pos < s.size() && s[pos] >= '0' && s[pos] <= '9') {
            if (fractionDigits < 3) milliseconds = milliseconds * 10 + (s[pos] - '0');
            ++fractionDigits; ++pos;
        }
        if (fractionDigits < 1 || fractionDigits > 9) throw std::invalid_argument("invalid ISO fraction");
        for (int i = fractionDigits; i < 3; ++i) milliseconds *= 10;
    }
    int offsetSeconds = 0;
    if (pos < s.size() && s[pos] == 'Z' && pos + 1 == s.size()) {
        ++pos;
    } else if (pos + 6 == s.size() && (s[pos] == '+' || s[pos] == '-') && s[pos + 3] == ':') {
        const int offsetHour = digits(s, pos + 1, 2), offsetMinute = digits(s, pos + 4, 2);
        if (offsetHour > 18 || offsetMinute > 59 || (offsetHour == 18 && offsetMinute != 0))
            throw std::invalid_argument("invalid ISO UTC offset");
        offsetSeconds = (offsetHour * 3600 + offsetMinute * 60) * (s[pos] == '+' ? 1 : -1);
        pos += 6;
    } else throw std::invalid_argument("invalid ISO timezone");
    if (pos != s.size()) throw std::invalid_argument("trailing ISO timestamp data");
    const long long days = std::chrono::sys_days{day}.time_since_epoch().count();
    return (days * 86400LL + hour * 3600 + minute * 60 + second - offsetSeconds) * 1000LL + milliseconds;
}
}

namespace Botcraft::Utilities {
long long int TimestampMilliFromISO8601(const std::string& s) {
    try {
        const long long value = parse(s);
        // Certificate validity dates are public metadata, never authentication secrets.
        return value;
    } catch (const std::exception&) {
        std::fprintf(stderr, "[AntiServerClose TimestampFix] Invalid certificate timestamp; failing closed.\n");
        return 0;
    }
}
}

#ifdef TIMESTAMP_FIX_TEST_MAIN
int main(int argc, char** argv) {
    if (argc != 2) return 2;
    std::printf("%lld\n", Botcraft::Utilities::TimestampMilliFromISO8601(argv[1]));
}
#endif
